package cpw.mods.inventorysorter;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.SidedProxy;

/**
 * Created by cpw on 08/01/16.
 */
public class SideProxy
{
    @SidedProxy(clientSide="cpw.mods.inventorysorter.SideProxy$ClientProxy", serverSide="cpw.mods.inventorysorter.SideProxy")
    static SideProxy INSTANCE;
    public void bindKeys()
    {

    }

    public static class ClientProxy extends SideProxy
    {
        @Override
        public void bindKeys()
        {
            MinecraftForge.EVENT_BUS.register(new KeyHandler());
        }
    }
}
