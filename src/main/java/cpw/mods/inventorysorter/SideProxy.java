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

import net.minecraftforge.common.*;
import net.minecraftforge.common.config.*;
import net.minecraftforge.fml.InterModComms;

import java.io.*;
import java.util.*;
import java.util.function.*;

/**
 * Created by cpw on 08/01/16.
 */
public class SideProxy
{
    Property containerDebug;
    Property containerBlacklist;
    private Configuration configuration;

    public void bindKeys()
    {

    }

    protected void doConfiguration(File suggestedConfigurationFile, Consumer<Configuration> thingsToDo) {
        if (configuration == null)
            configuration = new Configuration(suggestedConfigurationFile);
        thingsToDo.accept(configuration);
        if (configuration.hasChanged())
        {
            configuration.save();
        }

    }

    protected Consumer<Configuration> blackList() {
        return c-> {
            containerBlacklist = c.get(Configuration.CATEGORY_GENERAL, "containerBlacklist", new String[0]);
            for (String blacklisted : containerBlacklist.getStringList()) {
                InterModComms.sendTo("inventorysorter", "containerblacklist", ()->blacklisted);
            }
            containerDebug = c.get(Configuration.CATEGORY_GENERAL, "containerDebug", false);
            containerDebug.setLanguageKey("inventorysorter.gui.containerDebug");
            containerDebug.setRequiresMcRestart(false);
            containerDebug.setRequiresWorldRestart(false);
            InventorySorter.INSTANCE.debugLog = containerDebug.getBoolean(false);
        };
    }
    public void loadConfiguration(File suggestedConfigurationFile)
    {
        doConfiguration(suggestedConfigurationFile, blackList());
    }

    public void updateConfiguration(final Set<String> cbl) {
        containerBlacklist.set(cbl.toArray(new String[cbl.size()]));
        configuration.save();
    }

    public static class ClientProxy extends SideProxy
    {
        private KeyHandler keyHandler;

        @Override
        public void bindKeys()
        {
            keyHandler = new KeyHandler();
        }

        @Override
        public void loadConfiguration(File suggestedConfigurationFile)
        {
            doConfiguration(suggestedConfigurationFile, blackList().andThen(Action::configure));
/*
            MinecraftForge.EVENT_BUS.register(new Object() {
                @SubscribeEvent
                public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent evt)
                {
                    if (!"inventorysorter".equals(evt.getModID())) return;
                    doConfiguration(suggestedConfigurationFile, blackList().andThen(Action::configure));
                }
            });
*/

        }
    }
}
