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
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.*;
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

    static final Logger LOGGER = LogUtils.getLogger();
    ResourceLocation lastContainerType;
    boolean debugLog;

    final Set<String> slotblacklist = new HashSet<>();
    final Set<ResourceLocation> containerblacklist = new HashSet<>();
    final Set<String> imcSlotBlacklist = new HashSet<>();
    final Set<ResourceLocation> imcContainerBlacklist = new HashSet<>();

    public InventorySorter() {
        INSTANCE = this;
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::preinit);
        bus.addListener(this::handleimc);
        bus.addListener(this::onConfigLoad);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        COMMAND_ARGUMENT_TYPES.register(bus);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, ()->KeyHandler::init);
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

        this.slotblacklist.addAll(imcSlotBlacklist);
        this.containerblacklist.addAll(imcContainerBlacklist);
    }

    private void preinit(FMLCommonSetupEvent evt) {
        Network.init();
    }

    private void onServerStarting(ServerStartingEvent evt) {
        InventorySorterCommand.register(evt.getServer().getCommands().getDispatcher());
        this.slotblacklist.addAll(Config.CONFIG.slotBlacklist.get());
        this.containerblacklist.addAll(Config.CONFIG.containerBlacklist.get().stream().map(ResourceLocation::new).collect(Collectors.toSet()));
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
        Config.CONFIG.containerBlacklist.set(containerblacklist.stream().map(Objects::toString).collect(Collectors.toList()));
        Config.CONFIG.slotBlacklist.set(new ArrayList<>(slotblacklist));
        updateBlacklists();
    }

    void onConfigLoad(ModConfigEvent configEvent) {
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
        final var containerType = context.getArgument("container", ResourceLocation.class);
        if (ForgeRegistries.MENU_TYPES.containsKey(containerType)) {
            INSTANCE.containerblacklist.add(containerType);
            INSTANCE.updateConfig();
            context.getSource().sendSuccess(Component.translatable("inventorysorter.commands.inventorysorter.bladd.message", containerType), true);
            return 1;
        } else {
            context.getSource().sendSuccess(Component.translatable("inventorysorter.commands.inventorysorter.badtype", containerType), true);
            return 0;
        }
    }

    static int blackListRemove(final CommandContext<CommandSourceStack> context) {
        final var containerType = context.getArgument("container", ResourceLocation.class);
        if (ForgeRegistries.MENU_TYPES.containsKey(containerType) && INSTANCE.containerblacklist.remove(containerType)) {
            INSTANCE.updateConfig();
            context.getSource().sendSuccess(Component.translatable("inventorysorter.commands.inventorysorter.blremove.message", containerType), true);
            return 1;
        } else {
            context.getSource().sendSuccess(Component.translatable("inventorysorter.commands.inventorysorter.badtype", containerType), true);
            return 0;
        }
    }

    static int showLast(final CommandContext<CommandSourceStack> context) {
        if (INSTANCE.lastContainerType != null) {
            context.getSource().sendSuccess(Component.translatable("inventorysorter.commands.inventorysorter.showlast.message", INSTANCE.lastContainerType), true);
        } else {
            context.getSource().sendSuccess(Component.translatable("inventorysorter.commands.inventorysorter.showlast.nosort"), true);
        }
        return 0;
    }

    static int showBlacklist(final CommandContext<CommandSourceStack> context) {
        if (INSTANCE.containerblacklist.isEmpty()) {
            context.getSource().sendSuccess(Component.translatable("inventorysorter.commands.inventorysorter.showblacklist.empty"), true);
        } else {
            context.getSource().sendSuccess(Component.translatable("inventorysorter.commands.inventorysorter.showblacklist.message", listBlacklist().collect(Collectors.toList())), true);
        }
        return 0;
    }

    static Stream<ResourceLocation> listContainers() {
        return ForgeRegistries.MENU_TYPES.getEntries().stream().map(e -> e.getKey().location());
    }

    static Stream<ResourceLocation> listBlacklist() {
        return INSTANCE.containerblacklist.stream();
    }

    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registry.COMMAND_ARGUMENT_TYPE_REGISTRY, "inventorysorter");
    private static final RegistryObject<SingletonArgumentInfo<InventorySorterCommand.ContainerResourceLocationArgument>> CONTAINER_CLASS = COMMAND_ARGUMENT_TYPES.register("container_reslocation", ()-> ArgumentTypeInfos.registerByClass(InventorySorterCommand.ContainerResourceLocationArgument.class, SingletonArgumentInfo.contextFree(InventorySorterCommand.ContainerResourceLocationArgument::new)));
}
