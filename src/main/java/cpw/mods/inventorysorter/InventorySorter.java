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

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Properties;

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
        SideProxy.INSTANCE.loadConfiguration(evt.getSuggestedConfigurationFile());
        log = evt.getModLog();
        channel = NetworkRegistry.INSTANCE.newSimpleChannel("inventorysorter");
        channel.registerMessage(ServerHandler.class, Network.ActionMessage.class, 1, Side.SERVER);
        SideProxy.INSTANCE.bindKeys();
    }
}
