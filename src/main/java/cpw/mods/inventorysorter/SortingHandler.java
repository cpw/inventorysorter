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
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Created by cpw on 08/01/16.
 */
public enum SortingHandler implements Function<Action.ActionContext,Void>
{
    INSTANCE;
    @Nullable
    @Override
    public Void apply(@Nullable Action.ActionContext context)
    {
        if (context == null) throw new NullPointerException("WHUT");
        IInventory inv = context.slot.inventory;
        final Multiset<ItemStackHolder> itemcounts = InventoryHandler.INSTANCE.getInventoryContent(context);
        final UnmodifiableIterator<Multiset.Entry<ItemStackHolder>> itemsIterator = Multisets.copyHighestCountFirst(itemcounts).entrySet().iterator();
        int slotLow;
        int slotHigh;
        if (inv == context.player.inventory)
        {
            boolean isPlayerContainer = context.player.openContainer == context.player.inventoryContainer;
            boolean sourceHotBar = context.slot.getSlotIndex() < 9;
            InventoryHandler.InventoryMapping m = context.mapping.get(context.player.inventory);
            int offset = isPlayerContainer ? 4 : 0;
            slotLow = sourceHotBar ? m.end - 8 : m.begin + offset;
            slotHigh = sourceHotBar ? m.end + 1: m.end - 8;
        }
        else
        {
            InventoryHandler.InventoryMapping m = context.mapping.get(context.slot.inventory);
            slotLow = m.begin;
            slotHigh = m.end + 1;
        }

        Multiset.Entry<ItemStackHolder> stackHolder = itemsIterator.hasNext() ? itemsIterator.next() : null;
        int itemCount = stackHolder != null ? stackHolder.getCount() : 0;
        for (int i = slotLow; i < slotHigh; i++)
        {
            final Slot slot = context.player.openContainer.getSlot(i);
            ItemStack target = null;
            if (itemCount > 0 && stackHolder != null)
            {
                target = stackHolder.getElement().is.copy();
                target.stackSize = itemCount > target.getMaxStackSize() ? target.getMaxStackSize() : itemCount;
            }
            if ((target != null && !slot.isItemValid(target)) || !slot.canTakeStack(context.player)) continue;
            slot.putStack(target);
            itemCount-= (target != null ? target.stackSize : 0);
            if (itemCount ==0)
            {
                stackHolder = itemsIterator.hasNext() ? itemsIterator.next() : null;
                itemCount = stackHolder != null ? stackHolder.getCount() : 0;
            }
        }
        return null;
    }
}
