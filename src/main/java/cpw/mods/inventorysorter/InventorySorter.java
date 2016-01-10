package cpw.mods.inventorysorter;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Created by cpw on 08/01/16.
 */

@Mod(modid="inventorysorter",name="Inventory Sorter", version="1.0")
public class InventorySorter
{
    @Mod.Instance("inventorysorter")
    public static InventorySorter INSTANCE;

    public Logger log;
    public SimpleNetworkWrapper channel;


    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent evt)
    {
        loadConfig(evt.getSuggestedConfigurationFile());

        channel = NetworkRegistry.INSTANCE.newSimpleChannel("inventorysorter");
        channel.registerMessage(ServerHandler.class, Network.ActionMessage.class, 1, Side.SERVER);
        SideProxy.INSTANCE.bindKeys();
    }

    private void loadConfig(File suggestedConfigurationFile)
    {

    }
}
