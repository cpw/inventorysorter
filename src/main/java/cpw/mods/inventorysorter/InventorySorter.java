/*
 *     Copyright © 2016 cpw
 *     This file is part of Inventorysorter.
 *
 *     Inventorysorter is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Inventorysorter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Inventorysorter.  If not, see <http://www.gnu.org/licenses/>.
 */

package cpw.mods.inventorysorter;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by cpw on 08/01/16.
 */

@Mod("inventorysorter")
public class InventorySorter
{
    public static InventorySorter INSTANCE;

    static final Logger LOGGER = LogManager.getLogger();
    ResourceLocation lastContainerType;
    boolean debugLog;

    final Set<String> slotblacklist = new HashSet<>();
    final Set<ResourceLocation> containerblacklist = new HashSet<>();

    final Set<String> imcSlotBlacklist = new HashSet<>();
    final Set<ResourceLocation> imcContainerBlacklist = new HashSet<>();

    ItemOrdering itemOrdering = ItemOrdering.BY_COUNT;

    public InventorySorter() {
        INSTANCE = this;
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::preinit);
        bus.addListener(this::clientSetup);
        bus.addListener(this::handleimc);
        bus.addListener(this::onConfigLoad);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void clientSetup(FMLClientSetupEvent evt) {
        KeyHandler.init();
    }

    private void handleimc(final InterModProcessEvent evt)
    {
        final Stream<InterModComms.IMCMessage> imc = InterModComms.getMessages("inventorysorter");
        imc.forEach(this::handleIMCMessage);
    }

    /**
     * Supply an IMC of `slotblacklist` as {@link String} to add to the slot blacklist. This uses the complete class path.
     * For example `net.minecraft.world.inventory.Slot`
     * <p>
     * Supply an IMC of `containerblacklist as {@link ResourceLocation} to add to the container blacklist. This uses
     * the container / menu type, for example: `minecraft:generic_3x3`
     */
    private void handleIMCMessage(final InterModComms.IMCMessage msg) {
        if ("slotblacklist".equals(msg.method())) {
            Object message = msg.messageSupplier().get();
            if (message instanceof final String blackListTarget && imcSlotBlacklist.add(blackListTarget)) {
                debugLog("SlotBlacklist added {}", () -> new String[]{blackListTarget});
            } else {
                LOGGER.warn("Rejected slotblacklist due to bad messageSupplier type. Please supply a [String]");
            }
        }

        if ("containerblacklist".equals(msg.method())) {
            Object message = msg.messageSupplier().get();
            if (message instanceof final ResourceLocation blackListTarget && imcContainerBlacklist.add(blackListTarget)) {
                debugLog("ContainerBlacklist added {}", () -> new String[] {blackListTarget.toString()});
            } else {
                LOGGER.warn("Rejected containerblacklist due to bad messageSupplier type. Please supply a [ResourceLocation]");
            }
        }

        updateBlacklists();
    }

    private void preinit(FMLCommonSetupEvent evt) {
        Network.init();
    }

    private void onServerStarting(ServerStartingEvent evt) {
        InventorySorterCommand.register(evt.getServer().getCommands().getDispatcher());
    }

    private void updateBlacklists() {
        // Clear all entries
        this.slotblacklist.clear();
        this.containerblacklist.clear();

        // Merge in the config values and the imc values
        this.slotblacklist.addAll(Config.CONFIG.slotBlacklist.get());
        this.containerblacklist.addAll(Config.CONFIG.containerBlacklist.get().stream().map(ResourceLocation::new).collect(Collectors.toSet()));
        this.slotblacklist.addAll(imcSlotBlacklist);
        this.containerblacklist.addAll(imcContainerBlacklist);
    }

    private void updateConfig() {
        Config.CONFIG.containerBlacklist.set(containerblacklist.stream().filter(e -> !imcContainerBlacklist.contains(e)).map(Objects::toString).collect(Collectors.toList()));
        Config.CONFIG.slotBlacklist.set(slotblacklist.stream().filter(e -> !imcSlotBlacklist.contains(e)).collect(Collectors.toList()));
        Config.CONFIG.itemOrdering.set(itemOrdering);

        updateBlacklists();
    }

    void onConfigLoad(ModConfigEvent configEvent) {
		itemOrdering = Config.CONFIG.itemOrdering.get();
        updateBlacklists();
    }

    boolean wheelModConflicts() {
        return ModList.get().isLoaded("mousetweaks");
    }

    boolean sortingModConflicts() {
        return false;
    }

    final void debugLog(String message, Supplier<String[]> args) {
        if (debugLog) {
            LOGGER.error(message, (Object[]) args.get());
        }
    }

    static int blackListAdd(final CommandContext<CommandSourceStack> context) {
        final ResourceLocation containerType = InventorySorterCommand.Arguments.CONTAINER.get(context);
        if (ForgeRegistries.CONTAINERS.containsKey(containerType)) {
            INSTANCE.containerblacklist.add(containerType);
            INSTANCE.updateConfig();
            context.getSource().sendSuccess(new TranslatableComponent("inventorysorter.commands.inventorysorter.bladd.message", containerType), true);
            return 1;
        } else {
            context.getSource().sendSuccess(new TranslatableComponent("inventorysorter.commands.inventorysorter.badtype", containerType), true);
            return 0;
        }
    }

    static int blackListRemove(final CommandContext<CommandSourceStack> context) {
        final ResourceLocation containerType = InventorySorterCommand.Arguments.BLACKLISTED.get(context);
        if (ForgeRegistries.CONTAINERS.containsKey(containerType) && INSTANCE.containerblacklist.remove(containerType)) {
            INSTANCE.updateConfig();
            context.getSource().sendSuccess(new TranslatableComponent("inventorysorter.commands.inventorysorter.blremove.message", containerType), true);
            return 1;
        } else {
            context.getSource().sendSuccess(new TranslatableComponent("inventorysorter.commands.inventorysorter.badtype", containerType), true);
            return 0;
        }
    }

    static int showLast(final CommandContext<CommandSourceStack> context) {
        if (INSTANCE.lastContainerType != null) {
            context.getSource().sendSuccess(new TranslatableComponent("inventorysorter.commands.inventorysorter.showlast.message", INSTANCE.lastContainerType), true);
        } else {
            context.getSource().sendSuccess(new TranslatableComponent("inventorysorter.commands.inventorysorter.showlast.nosort"), true);
        }
        return 0;
    }

    static int showBlacklist(final CommandContext<CommandSourceStack> context) {
        if (INSTANCE.containerblacklist.isEmpty()) {
            context.getSource().sendSuccess(new TranslatableComponent("inventorysorter.commands.inventorysorter.showblacklist.empty"), true);
        } else {
            context.getSource().sendSuccess(new TranslatableComponent("inventorysorter.commands.inventorysorter.showblacklist.message", listBlacklist().collect(Collectors.toList())), true);
        }
        return 0;
    }

    static int setItemOrdering(final CommandContext<CommandSourceStack> context) {
        final ItemOrdering newOrdering = InventorySorterCommand.Arguments.ORDERING.get(context);
        INSTANCE.itemOrdering = newOrdering;
        INSTANCE.updateConfig();
        context.getSource().sendSuccess(new TranslatableComponent("inventorysorter.commands.inventorysorter.setorder.ok", newOrdering.toString()), true);
        return 0;
    }

    static Stream<String> listContainers() {
        return ForgeRegistries.CONTAINERS.getEntries().stream().map(e -> e.getKey().location().toString());
    }

    static Stream<String> listBlacklist() {
        return INSTANCE.containerblacklist.stream().map(Object::toString);
    }
}
