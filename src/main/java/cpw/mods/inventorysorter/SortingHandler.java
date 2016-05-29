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
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.TreeMultiset;
import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLLog;
import org.apache.logging.log4j.Level;

import javax.annotation.Nullable;

/**
 * @author cpw
 */
public enum SortingHandler implements Function<Action.ActionContext,Void>
{
    INSTANCE;
    @Nullable
    @Override
    public Void apply(@Nullable Action.ActionContext context)
    {
        if (context == null) throw new NullPointerException("WHUT");
        // Ignore if we can't find ourselves in the slot set
        if (context.slotMapping == null) return null;
        final Multiset<ItemStackHolder> itemcounts = InventoryHandler.INSTANCE.getInventoryContent(context);

        if (context.slot.inventory instanceof InventoryCrafting)
        {
            distributeInventory(context, itemcounts);
        }
        else
        {
            compactInventory(context, itemcounts);
        }
        return null;
    }

    private void distributeInventory(Action.ActionContext context, Multiset<ItemStackHolder> itemcounts)
    {
        InventoryCrafting ic = (InventoryCrafting)context.slot.inventory;
        Multiset<ItemStackHolder> slotCounts = TreeMultiset.create(new InventoryHandler.ItemStackComparator());
        for (int x=0; x<ic.getWidth(); x++)
        {
            for (int y=0; y<ic.getHeight(); y++)
            {
                ItemStack is = ic.getStackInRowAndColumn(x, y);
                if (is != null)
                {
                    slotCounts.add(new ItemStackHolder(is));
                }
            }
        }

        final ImmutableMultiset<ItemStackHolder> staticcounts = ImmutableMultiset.copyOf(itemcounts);
        for (int x=0; x<ic.getWidth(); x++)
        {
            for (int y = 0; y < ic.getHeight(); y++)
            {
                ItemStack is = ic.getStackInRowAndColumn(x, y);
                if (is != null)
                {
                    ItemStackHolder ish = new ItemStackHolder(is);
                    int count = staticcounts.count(ish);
                    int slotNum = slotCounts.count(ish);
                    final int occurrences = count / slotNum;
                    itemcounts.remove(ish, occurrences);
                    is.stackSize = occurrences;
                }
            }
        }
        for (int x=0; x<ic.getWidth(); x++)
        {
            for (int y = 0; y < ic.getHeight(); y++)
            {
                ItemStack is = ic.getStackInRowAndColumn(x, y);
                if (is != null)
                {
                    ItemStackHolder ish = new ItemStackHolder(is);
                    if (itemcounts.count(ish) > 0)
                    {
                        is.stackSize+=itemcounts.setCount(ish,0);
                    }
                }
            }
        }
        for (int slot = context.slotMapping.begin; slot < context.slotMapping.end + 1; slot++)
        {
            context.player.openContainer.getSlot(slot).onSlotChanged();
        }
    }
    private void compactInventory(Action.ActionContext context, Multiset<ItemStackHolder> itemcounts)
    {
        final UnmodifiableIterator<Multiset.Entry<ItemStackHolder>> itemsIterator;
        try
        {
            itemsIterator = Multisets.copyHighestCountFirst(itemcounts).entrySet().iterator();
        }
        catch (Exception e)
        {
            FMLLog.log(Level.WARN, e, "Weird, the sorting didn't quite work!");
            return;
        }
        int slotLow = context.slotMapping.begin;
        int slotHigh = context.slotMapping.end + 1;

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
    }
}
