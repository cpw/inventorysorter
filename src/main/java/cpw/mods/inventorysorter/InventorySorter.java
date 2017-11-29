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

import com.google.common.collect.*;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.*;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.*;
import org.apache.logging.log4j.*;

import java.util.*;

/**
 * Created by cpw on 08/01/16.
 */

@Mod(modid="inventorysorter",name="Inventory Sorter", guiFactory = "cpw.mods.inventorysorter.GuiConfigFactory")
public class InventorySorter
{
    @Mod.Instance("inventorysorter")
    public static InventorySorter INSTANCE;

    public Logger log;
    public SimpleNetworkWrapper channel;
    final List slotblacklist = Lists.newArrayList();

    @Mod.EventHandler
    public void handleimc(FMLInterModComms.IMCEvent evt)
    {
        for (FMLInterModComms.IMCMessage msg : evt.getMessages())
        {
            if ("slotblacklist".equals(msg.key) && msg.isStringMessage()) {
                slotblacklist.add(msg.getStringValue());
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

    boolean wheelModConflicts() {
        return Loader.isModLoaded("mousetweaks");
    }

    boolean sortingModConflicts() {
        return false;
    }
}
