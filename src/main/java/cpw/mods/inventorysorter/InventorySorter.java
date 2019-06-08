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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Created by cpw on 08/01/16.
 */

@Mod("inventorysorter")
public class InventorySorter
{
    public static InventorySorter INSTANCE;

    static final Logger LOGGER = LogManager.getLogger();
    boolean debugLog;
    final Set<String> slotblacklist = new HashSet<>();
    final Set<String> containerblacklist = new HashSet<>();
    String containerTracking;


    public InventorySorter() {
        INSTANCE = this;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preinit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
//        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::handleimc);
    }


    void clientSetup(FMLClientSetupEvent evt) {
        KeyHandler.init();
    }
//    public void handleimc(final FMLPostInitializationEvent evt)
//    {
//        final Stream<InterModComms.IMCMessage> imc = InterModComms.getMessages("inventorysorter");
//        imc.forEach(this::handleimcmessage);
//
//    }

    private void handleimcmessage(final InterModComms.IMCMessage msg) {
        if ("slotblacklist".equals(msg.getMethod())) {
            final String slotBlacklistTarget = msg.<String>getMessageSupplier().get();
            if (slotblacklist.add(slotBlacklistTarget)) {
                debugLog("SlotBlacklist added {}", ()->new String[] {slotBlacklistTarget});
            }
        }

        if ("containerblacklist".equals(msg.getMethod())) {
            final String slotContainerTarget = msg.<String>getMessageSupplier().get();
            if (containerblacklist.add(slotContainerTarget)) {
                debugLog("ContainerBlacklist added {}", () -> new String[]{slotContainerTarget});
            }
        }
    }

    void preinit(FMLCommonSetupEvent evt)
    {
//        SideProxy.INSTANCE.loadConfiguration(evt.getSuggestedConfigurationFile());
        Network.init();
        // blacklist codechickencore because
        InterModComms.sendTo("inventorysorter", "slotblacklist", ()->"codechicken.core.inventory.SlotDummy");
    }

    @SubscribeEvent
    public void onserverstarting(FMLServerStartingEvent evt) {
//        evt.registerServerCommand(new InventorySorterCommand());
    }
    boolean wheelModConflicts() {
        return ModList.get().isLoaded("mousetweaks");
    }

    boolean sortingModConflicts() {
        return false;
    }

    public final void debugLog(String message, Supplier<String[]> args) {
        if (debugLog) {
            LOGGER.error(message, (Object[]) args.get());
        }
    }

/*
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
*/

    public static TranslationTextComponent showBlacklist() {
        if (InventorySorter.INSTANCE.containerblacklist.isEmpty()) {
            return new TranslationTextComponent("inventorysorter.commands.inventorysorter.list.empty");
        } else {
            return new TranslationTextComponent("inventorysorter.commands.inventorysorter.list.message", InventorySorter.INSTANCE.containerblacklist.stream().map(s -> "\"§a" + s + "§f\"").collect(Collectors.joining(",")));
        }
    }

    private static StringTextComponent greenText(final String string) {
        final StringTextComponent tcs = new StringTextComponent(string);
        tcs.getStyle().setColor(TextFormatting.GREEN);
        return tcs;
    }
}
