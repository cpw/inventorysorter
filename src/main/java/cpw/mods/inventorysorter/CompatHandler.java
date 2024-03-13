package cpw.mods.inventorysorter;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.Level;

public class CompatHandler
{
    public static boolean isMouseTweaksLoaded()
    {
        return ModList.get().isLoaded("mousetweaks");
    }
}
