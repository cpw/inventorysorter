package cpw.mods.inventorysorter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by cpw on 29/05/16.
 */
public class GuiConfigFactory implements IModGuiFactory
{
    public static class ConfigGui extends GuiConfig {
        public ConfigGui(GuiScreen parent)
        {
            super(parent, getConfigElements(), "inventorysorter", false, false, I18n.format("inventorysorter.gui.title"));
        }

        private static List<IConfigElement> getConfigElements()
        {
            List<IConfigElement> list = Lists.newArrayList();

            Set<String> actionNames = Sets.newHashSet();
            for (Action a : Action.values())
            {
                if (actionNames.add(a.getConfigName()))
                {
                    final ConfigElement configElement = new ConfigElement(a.getProperty());
                    list.add(configElement);
                }
            }
            return list;
        }
    }
    @Override
    public void initialize(Minecraft minecraftInstance)
    {

    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass()
    {
        return ConfigGui.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories()
    {
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element)
    {
        return null;
    }
}
