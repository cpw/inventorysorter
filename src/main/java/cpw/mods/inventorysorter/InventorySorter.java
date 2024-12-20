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
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by cpw on 08/01/16.
 */

@Mod("inventorysorter")
public class InventorySorter {
    public static InventorySorter INSTANCE;

    static final Logger LOGGER = LogManager.getLogger();
    ResourceLocation lastContainerType;
    boolean debugLog;
    private final Set<String> slotblacklist = new HashSet<>();
    private final Set<String> containerblacklist = new HashSet<>();
    private boolean dodgeMouseTweaks;

    public InventorySorter(IEventBus modBus, ModContainer me) {
        INSTANCE = this;
        final IEventBus bus = modBus;
        bus.addListener(this::handleimc);
        bus.addListener(this::onConfigLoad);
        Config.register(me);
        COMMAND_ARGUMENT_TYPES.register(bus);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        KeyHandler.registerKeyHandlers(bus);
        Network.registerPayloadHandlers(bus);
    }

    private void handleimc(final InterModProcessEvent evt) {
        final Stream<InterModComms.IMCMessage> imc = InterModComms.getMessages("inventorysorter");
        imc.forEach(this::handleimcmessage);
    }

    private void handleimcmessage(final InterModComms.IMCMessage msg) {
        if ("slotblacklist".equals(msg.method())) {
            final String slotBlacklistTarget = (String) msg.messageSupplier().get();
            if (slotblacklist.add(slotBlacklistTarget)) {
                debugLog("SlotBlacklist added {}", ()->new String[] {slotBlacklistTarget});
            }
        }

        if ("containerblacklist".equals(msg.method())) {
            final ResourceLocation slotContainerTarget = (ResourceLocation) msg.messageSupplier().get();
            if (containerblacklist.add(slotContainerTarget.toString())) {
                debugLog("ContainerBlacklist added {}", () -> new String[] {slotContainerTarget.toString()});
            }
        }
    }

    private void updateConfig() {
        Config.ServerConfig.CONFIG.containerBlacklist.set(new ArrayList<>(containerblacklist));
        Config.ServerConfig.CONFIG.slotBlacklist.set(new ArrayList<>(slotblacklist));
        Config.ServerConfig.SPEC.save();
    }

    private void onServerStarting(ServerStartingEvent evt) {
        InventorySorterCommand.register(evt.getServer().getCommands().getDispatcher());
    }

    boolean isSlotBlacklisted(Slot slot) {
        return slotblacklist.contains(slot.getClass().getName()) || Config.ServerConfig.CONFIG.slotBlacklist.get().contains(slot.getClass().getName());
    }

    boolean isContainerBlacklisted(ResourceLocation container) {
        return containerblacklist.contains(container.toString()) || Config.ServerConfig.CONFIG.containerBlacklist.get().contains(container.toString());
    }

    void onConfigLoad(ModConfigEvent configEvent) {
        // Don't load data on unloading
        if (configEvent instanceof ModConfigEvent.Unloading) {
            return;
        }

        switch (configEvent.getConfig().getType()) {
            case SERVER:
                this.slotblacklist.addAll(Config.ServerConfig.CONFIG.slotBlacklist.get());
                this.containerblacklist.addAll(Config.ServerConfig.CONFIG.containerBlacklist.get());
                break;
            case CLIENT:
                if (Config.ClientConfig.CONFIG.dodgeMousetweaks.get() && ModList.get().isLoaded("mousetweaks")) {
                    this.dodgeMouseTweaks = true;
                }
                break;
        }

    }

    final void debugLog(String message, Supplier<String[]> args) {
        if (debugLog) {
            LOGGER.error(message, (Object[]) args.get());
        }
    }

    private static Component greenText(final String string) {
        final Component tcs = Component.translatable(string);
        tcs.getStyle().withColor(ChatFormatting.GREEN);
        return tcs;
    }

    static int blackListAdd(final CommandContext<CommandSourceStack> context) {
        final var containerType = context.getArgument("container", ResourceLocation.class);
        if (BuiltInRegistries.MENU.containsKey(containerType)) {
            INSTANCE.containerblacklist.add(containerType.toString());
            INSTANCE.updateConfig();
            context.getSource().sendSuccess(()->Component.translatable("inventorysorter.commands.inventorysorter.bladd.message", containerType.toString()), true);
            return 1;
        } else {
            context.getSource().sendSuccess(()->Component.translatable("inventorysorter.commands.inventorysorter.badtype", containerType), true);
            return 0;
        }
    }

    static int blackListRemove(final CommandContext<CommandSourceStack> context) {
        final var containerType = context.getArgument("container", ResourceLocation.class);
        if (BuiltInRegistries.MENU.containsKey(containerType) && INSTANCE.containerblacklist.remove(containerType.toString())) {
            INSTANCE.updateConfig();
            context.getSource().sendSuccess(()->Component.translatable("inventorysorter.commands.inventorysorter.blremove.message", containerType.toString()), true);
            return 1;
        } else {
            context.getSource().sendSuccess(()->Component.translatable("inventorysorter.commands.inventorysorter.badtype", containerType.toString()), true);
            return 0;
        }
    }

    static int showLast(final CommandContext<CommandSourceStack> context) {
        if (INSTANCE.lastContainerType != null) {
            context.getSource().sendSuccess(()->Component.translatable("inventorysorter.commands.inventorysorter.showlast.message", INSTANCE.lastContainerType.toString()), true);
        } else {
            context.getSource().sendSuccess(()->Component.translatable("inventorysorter.commands.inventorysorter.showlast.nosort"), true);
        }
        return 0;
    }

    static int showBlacklist(final CommandContext<CommandSourceStack> context) {
        if (INSTANCE.containerblacklist.isEmpty()) {
            context.getSource().sendSuccess(()->Component.translatable("inventorysorter.commands.inventorysorter.showblacklist.empty"), true);
        } else {
            context.getSource().sendSuccess(()->Component.translatable("inventorysorter.commands.inventorysorter.showblacklist.message", listBlacklist()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.joining(", "))), true);
        }
        return 0;
    }

    static Stream<ResourceLocation> listContainers() {
        return BuiltInRegistries.MENU.entrySet().stream().map(e->e.getKey().location());
    }

    static Stream<ResourceLocation> listBlacklist() {
        return INSTANCE.containerblacklist.stream().map(ResourceLocation::parse);
    }

    boolean dontDodgeMouseTweaks() {
        return !dodgeMouseTweaks;
    }

    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, "inventorysorter");
    private static final DeferredHolder<ArgumentTypeInfo<?,?>, SingletonArgumentInfo<InventorySorterCommand.ContainerResourceLocationArgument>> CONTAINER_CLASS = COMMAND_ARGUMENT_TYPES.register("container_reslocation",
            ()-> ArgumentTypeInfos.registerByClass(InventorySorterCommand.ContainerResourceLocationArgument.class, SingletonArgumentInfo.contextFree(InventorySorterCommand.ContainerResourceLocationArgument::new)));
}
