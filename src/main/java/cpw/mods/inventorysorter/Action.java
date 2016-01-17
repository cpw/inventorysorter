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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import java.util.Map;

/**
 * Created by cpw on 08/01/16.
 */
public enum Action
{
    SORT(SortingHandler.INSTANCE),
    ONEITEMIN(ScrollWheelHandler.ONEITEMIN),
    ONEITEMOUT(ScrollWheelHandler.ONEITEMOUT),
    ALL(AllItemsHandler.INSTANCE);

    private final Function<ActionContext, Void> worker;

    Action(Function<ActionContext, Void> worker)
    {
        this.worker = worker;
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

    public static class ActionContext
    {
        public final Slot slot;
        public final EntityPlayerMP player;
        public final ImmutableMap<IInventory, InventoryHandler.InventoryMapping> mapping;

        public ActionContext(Slot slot, EntityPlayerMP playerEntity)
        {
            this.slot = slot;
            this.player = playerEntity;
            Map<IInventory, InventoryHandler.InventoryMapping> mapping = Maps.newHashMap();
            for (Slot sl : playerEntity.openContainer.inventorySlots)
            {
                if (!mapping.containsKey(sl.inventory))
                {
                    mapping.put(sl.inventory, new InventoryHandler.InventoryMapping(sl.inventory, playerEntity.openContainer));
                }
                mapping.get(sl.inventory).begin = Math.min(sl.slotNumber, mapping.get(sl.inventory).begin);
                mapping.get(sl.inventory).end = Math.max(sl.slotNumber, mapping.get(sl.inventory).end);
            }
            this.mapping = ImmutableMap.copyOf(mapping);
        }
    }
}
