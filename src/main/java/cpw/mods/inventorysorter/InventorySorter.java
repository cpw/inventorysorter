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

import net.minecraft.util.text.*;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.*;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.*;
import org.apache.logging.log4j.*;

import java.util.*;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.*;

/**
 * Created by cpw on 08/01/16.
 */

@Mod(modid="inventorysorter",name="Inventory Sorter", guiFactory = "cpw.mods.inventorysorter.GuiConfigFactory")
public class InventorySorter
{
    @Mod.Instance("inventorysorter")
    public static InventorySorter INSTANCE;

    public Logger log;
    boolean debugLog;
    public SimpleNetworkWrapper channel;
    final Set<String> slotblacklist = new HashSet<>();
    final Set<String> containerblacklist = new HashSet<>();
    String containerTracking;

    @Mod.EventHandler
    public void handleimc(final FMLInterModComms.IMCEvent evt)
    {
        handleimcmessages(Optional.ofNullable(evt.getMessages()));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void handleimcmessages(final Optional<List<FMLInterModComms.IMCMessage>> messages) {
        messages.ifPresent(m->m.forEach(this::handleimcmessage));
    }

    private void handleimcmessage(final FMLInterModComms.IMCMessage msg) {
        if ("slotblacklist".equals(msg.key) && msg.isStringMessage()) {
            if (slotblacklist.add(msg.getStringValue())) {
                debugLog("SlotBlacklist added {}", ()->new String[] {msg.getStringValue()});
            }
        }
        if ("containerblacklist".equals(msg.key) && msg.isStringMessage()) {
            if (containerblacklist.add(msg.getStringValue())) {
                debugLog("ContainerBlacklist added {}", () -> new String[]{msg.getStringValue()});
            }
        }
    }

    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent evt)
    {
        final Properties versionProperties = evt.getVersionProperties();
        if (versionProperties != null)
        {
            evt.getModMetadata().version = versionProperties.getProperty("inventorysorter.version");
        }
        else
        {
            evt.getModMetadata().version = "1.0";
        }
        log = evt.getModLog();
        SideProxy.INSTANCE.loadConfiguration(evt.getSuggestedConfigurationFile());
        channel = NetworkRegistry.INSTANCE.newSimpleChannel("inventorysorter");
        channel.registerMessage(ServerHandler.class, Network.ActionMessage.class, 1, Side.SERVER);
        SideProxy.INSTANCE.bindKeys();
        // blacklist codechickencore because
        FMLInterModComms.sendMessage("inventorysorter", "slotblacklist", "codechicken.core.inventory.SlotDummy");
    }

    @Mod.EventHandler
    public void onserverstarting(FMLServerStartingEvent evt) {
        evt.registerServerCommand(new InventorySorterCommand());
    }
    boolean wheelModConflicts() {
        return Loader.isModLoaded("mousetweaks");
    }

    boolean sortingModConflicts() {
        return false;
    }

    public final void debugLog(String message, Supplier<String[]> args) {
        if (debugLog) {
            log.error(message, (Object[]) args.get());
        }
    }

    String modifyBlackList(Function<Set<String>, Boolean> modifier) {
        if (containerTracking != null) {
            if (modifier.apply(containerblacklist)) {
                SideProxy.INSTANCE.updateConfiguration(containerblacklist);
            }
            return containerTracking;
        }
        return null;
    }

    public static TextComponentTranslation blackListAdd() {
        final String blacklist = InventorySorter.INSTANCE.modifyBlackList(sa-> sa.add(InventorySorter.INSTANCE.containerTracking));
        if (blacklist != null) {
            return new TextComponentTranslation("inventorysorter.commands.inventorysorter.bladd.message", greenText(blacklist));
        } else {
            return InventorySorterCommand.NOOP_COMMAND;
        }
    }

    public static TextComponentTranslation blackListRemove() {
        final String blacklist = InventorySorter.INSTANCE.modifyBlackList(sa-> sa.remove(InventorySorter.INSTANCE.containerTracking));
        if (blacklist != null) {
            return new TextComponentTranslation("inventorysorter.commands.inventorysorter.blremove.message", greenText(blacklist));
        } else {
            return InventorySorterCommand.NOOP_COMMAND;
        }
    }

    public static TextComponentTranslation showLast() {
        if (InventorySorter.INSTANCE.containerTracking != null) {
           return new TextComponentTranslation("inventorysorter.commands.inventorysorter.show.message", greenText(InventorySorter.INSTANCE.containerTracking));
        }
        return InventorySorterCommand.NOOP_COMMAND;
    }

    public static TextComponentTranslation showBlacklist() {
        if (InventorySorter.INSTANCE.containerblacklist.isEmpty()) {
            return new TextComponentTranslation("inventorysorter.commands.inventorysorter.list.empty");
        } else {
            return new TextComponentTranslation("inventorysorter.commands.inventorysorter.list.message", InventorySorter.INSTANCE.containerblacklist.stream().map(s -> "\"§a" + s + "§f\"").collect(Collectors.joining(",")));
        }
    }

    private static TextComponentString greenText(final String string) {
        final TextComponentString tcs = new TextComponentString(string);
        tcs.getStyle().setColor(TextFormatting.GREEN);
        return tcs;
    }
}
