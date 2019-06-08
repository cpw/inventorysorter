package cpw.mods.inventorysorter;

/**
 * Created by cpw on 29/05/16.
 */
@SuppressWarnings("unused")
public class GuiConfigFactory //implements IModGuiFactory
{
/*
    public static class ConfigGui extends GuiConfig {
        public ConfigGui(GuiScreen parent)
        {
            super(parent, getConfigElements(), "inventorysorter", false, false, I18n.format("inventorysorter.gui.title"));
        }

        private static List<IConfigElement> getConfigElements()
        {
            List<IConfigElement> list = Lists.newArrayList();

            list.add(new ConfigElement(SideProxy.INSTANCE.containerDebug));
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
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(final GuiScreen parentScreen) {
        return new ConfigGui(parentScreen);
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories()
    {
        return null;
    }
*/
}
