package cpw.mods.inventorysorter;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.Level;

public class CompatHandler
{
    public static void init()
    {
        if(isMouseTweaksLoaded())
        {
            if(FMLEnvironment.dist == Dist.CLIENT)
            {
                InventorySorter.LOGGER.log(Level.INFO, "Mouse Tweaks found, Disabling wheel move module");
                Config.ClientConfig.CONFIG.wheelmoveModule.set(false);
            }
        }
    }

    public static boolean isMouseTweaksLoaded()
    {
        return ModList.get().isLoaded("mousetweaks");
    }
}
