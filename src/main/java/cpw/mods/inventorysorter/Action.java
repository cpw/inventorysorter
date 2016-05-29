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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.util.Map;

/**
 * Created by cpw on 08/01/16.
 */
public enum Action
{
    SORT(SortingHandler.INSTANCE, "middleClickSorting", "Middle-click sorting module", true),
    ONEITEMIN(ScrollWheelHandler.ONEITEMIN, "mouseWheelMoving", "Mouse wheel movement module", true),
    ONEITEMOUT(ScrollWheelHandler.ONEITEMOUT, "mouseWheelMoving", "Mouse wheel movement module", true),
    ALL(AllItemsHandler.INSTANCE, "allMoving", "All items movement module - NYI", false);

    private final Function<ActionContext, Void> worker;
    private final String configName;
    private boolean actionActive;
    private Property property;
    private final String comment;
    private final boolean implemented;

    Action(Function<ActionContext, Void> worker, String configName, String comment, boolean implemented)
    {
        this.worker = worker;
        this.configName = configName;
        this.comment = comment;
        this.implemented = implemented;
    }

    public static void configure(Configuration config)
    {
        for (Action a : values())
        {
            a.property = config.get(Configuration.CATEGORY_CLIENT, a.configName, true);
            a.property.setRequiresMcRestart(false);
            a.property.setRequiresWorldRestart(false);
            a.property.setLanguageKey("inventorysorter.gui." + a.configName);
            a.property.setShowInGui(a.implemented);
            a.property.setComment(a.comment);
            a.actionActive = a.property.getBoolean(true);
        }
    }
    public static Action interpret(KeyHandler.KeyStates keyStates)
    {
        if (keyStates.isDownClick()) return null;
        if (keyStates.isMiddleMouse()) return SORT;
        if (keyStates.mouseWheelRollingDown()) return ONEITEMIN;
        if (keyStates.mouseWheelRollingUp()) return ONEITEMOUT;
        if (keyStates.isSpace()) return ALL;
        return null;
    }
    public Network.ActionMessage message(Slot slot)
    {
        return new Network.ActionMessage(this, slot.slotNumber);
    }

    public void execute(ActionContext context)
    {
        this.worker.apply(context);
    }

    public Property getProperty()
    {
        return property;
    }

    public boolean isActive()
    {
        return actionActive;
    }

    public String getConfigName()
    {
        return configName;
    }

    public static class ActionContext
    {
        public final Slot slot;
        public final InventoryHandler.InventoryMapping slotMapping;
        public final EntityPlayerMP player;
        public final ImmutableBiMap<IInventory, InventoryHandler.InventoryMapping> mapping;

        public static final IInventory PLAYER_HOTBAR = new InventoryBasic("Dummy Hotbar", false, 0);
        public static final IInventory PLAYER_MAIN = new InventoryBasic("Dummy Main", false, 0);
        public static final IInventory PLAYER_OFFHAND = new InventoryBasic("Dummy Offhand", false, 0);

        public ActionContext(Slot slot, EntityPlayerMP playerEntity)
        {
            this.slot = slot;
            this.player = playerEntity;
            InventoryHandler.InventoryMapping slotTarget = null;
            Map<IInventory, InventoryHandler.InventoryMapping> mapping = Maps.newHashMap();
            InventoryHandler.InventoryMapping inventoryMapping = null;
            final Container openContainer = playerEntity.openContainer;
            for (Slot sl : openContainer.inventorySlots)
            {
                // Skip slots without an inventory - they're probably dummy slots
                if (sl.inventory == null) continue;
                if (!mapping.containsKey(sl.inventory))
                {
                    mapping.put(sl.inventory, new InventoryHandler.InventoryMapping(sl.inventory, openContainer));
                }
                inventoryMapping = mapping.get(sl.inventory);
                inventoryMapping.begin = Math.min(sl.slotNumber, inventoryMapping.begin);
                inventoryMapping.end = Math.max(sl.slotNumber, inventoryMapping.end);
                if (sl == slot)
                {
                    slotTarget = inventoryMapping;
                }
            }

            if (mapping.containsKey(playerEntity.inventory)) {
                final InventoryHandler.InventoryMapping playerMapping = mapping.remove(playerEntity.inventory);
                int mainStart = 9;
                int mainEnd = 36;
                int offhandStart = 40;

                InventoryHandler.InventoryMapping hotbarMapping = new InventoryHandler.InventoryMapping(PLAYER_HOTBAR, openContainer, playerEntity.inventory);
                InventoryHandler.InventoryMapping mainMapping = new InventoryHandler.InventoryMapping(PLAYER_MAIN, openContainer, playerEntity.inventory);
                InventoryHandler.InventoryMapping offhandMapping = new InventoryHandler.InventoryMapping(PLAYER_OFFHAND, openContainer, playerEntity.inventory);

                for (int i = playerMapping.begin; i<=playerMapping.end; i++)
                {
                    Slot s = openContainer.getSlot(i);
                    if (s.getSlotIndex() < mainStart)
                    {
                        hotbarMapping.begin = Math.min(s.slotNumber, hotbarMapping.begin);
                        hotbarMapping.end = Math.max(s.slotNumber, hotbarMapping.end);
                        mapping.put(PLAYER_HOTBAR, hotbarMapping);
                        inventoryMapping = hotbarMapping;
                    }
                    else if (s.getSlotIndex() < mainEnd)
                    {
                        mainMapping.begin = Math.min(s.slotNumber, mainMapping.begin);
                        mainMapping.end = Math.max(s.slotNumber, mainMapping.end);
                        mapping.put(PLAYER_MAIN, mainMapping);
                        inventoryMapping = mainMapping;
                    }
                    else if (s.getSlotIndex() >= offhandStart)
                    {
                        offhandMapping.begin = Math.min(s.slotNumber, offhandMapping.begin);
                        offhandMapping.end = Math.max(s.slotNumber, offhandMapping.end);
                        mapping.put(PLAYER_OFFHAND, offhandMapping);
                        inventoryMapping = offhandMapping;
                    }
                    if (s == slot)
                    {
                        slotTarget = inventoryMapping;
                    }
                }
            }
            this.slotMapping = slotTarget;
            this.mapping = ImmutableBiMap.copyOf(mapping);
        }
    }
}
