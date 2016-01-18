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

import com.google.common.base.Objects;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import com.google.common.primitives.Ints;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Map;

/**
 * Created by cpw on 09/01/16.
 */
public enum InventoryHandler
{
    INSTANCE;
    public final Method mergeStack = getMergeStackMethod();

    private Method getMergeStackMethod()
    {
        try
        {
            Method m = ReflectionHelper.findMethod(Container.class, null, new String[] { "func_"+"75135_a","mergeItemStack" }, ItemStack.class, int.class, int.class, boolean.class);
            m.setAccessible(true);
            return m;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public boolean mergeStack(Container container, ItemStack stack, int low, int high, boolean rev)
    {
        try
        {
            //noinspection ConstantConditions
            return (Boolean)mergeStack.invoke(container, stack, low, high, rev);
        } catch (Exception e)
        {
            return false;
        }
    }

    public ItemStack getItemStack(Action.ActionContext ctx)
    {
        return getItemStack(ctx.slot);
    }

    public ItemStack getItemStack(Slot slot)
    {
        return slot.inventory.getStackInSlot(slot.getSlotIndex());
    }

    public void moveItemToOtherInventory(Slot origin, Action.ActionContext ctx, Map<IInventory,InventoryMapping> mapping, ItemStack is, boolean rev)
    {
        int targetLow = 0;
        int targetHigh = 0;
        boolean forcedSlot = false;
        if (!rev && ctx.slot.getStack().getMaxStackSize() > ctx.slot.getStack().stackSize)
        {
            targetLow = ctx.slot.slotNumber;
            targetHigh = ctx.slot.slotNumber+1;
            forcedSlot = true;
        }
        if (ctx.player.inventoryContainer == ctx.player.openContainer && !forcedSlot)
        {
            boolean sourceHotBar = origin.slotNumber >= 36 && origin.slotNumber < 45;
            targetLow = sourceHotBar ? 9 : 35;
            targetHigh = sourceHotBar ? 35 : 44;
        }
        else if (origin.inventory == ctx.player.inventory && !forcedSlot)
        {
            for (Map.Entry<IInventory, InventoryMapping> m : mapping.entrySet())
            {
                if (m.getKey() != ctx.player.inventory)
                {
                    targetLow = m.getValue().begin;
                    targetHigh = m.getValue().end;
                }
            }
        }
        else if (!forcedSlot)
        {
            InventoryMapping m = mapping.get(ctx.player.inventory);
            targetLow = m.begin;
            targetHigh = m.end;
        }

        int rng = targetHigh - targetLow;
        for (int i = 0; i < rng; i++)
        {
            int slNum = rev ? targetHigh - i : targetLow + i;
            if (!ctx.player.openContainer.getSlot(slNum).isItemValid(is))
            {
                continue;
            }
            if (mergeStack(ctx.player.openContainer, is, slNum, slNum+1, !rev))
            {
                break;
            }
        }
    }

    public Slot findStackWithItem(ItemStack is, Action.ActionContext ctx, Map<IInventory,InventoryMapping> mapping, Slot origin)
    {
        if (is.getMaxStackSize() == 1) return null;

        if (ctx.player.inventoryContainer == ctx.player.openContainer)
        {
            boolean sourceHotBar = origin.getSlotIndex() < 9;
            int searchLow = sourceHotBar ? 9 : 36;
            int searchHigh = sourceHotBar ? 36 : 45;
            for (int i = searchLow; i < searchHigh; i++)
            {
                ItemStack sis = ctx.player.inventoryContainer.getSlot(i).getStack();
                if (sis != null && sis.getItem() == is.getItem() && ItemStack.areItemStackTagsEqual(sis, is))
                {
                    return ctx.player.openContainer.getSlot(i);
                }
            }
            return null;
        }
        for (Map.Entry<IInventory, InventoryMapping> ent : ctx.mapping.entrySet())
        {
            IInventory inv = ent.getKey();
            if (inv == origin.inventory) continue;
            for (int i = 0; i < inv.getSizeInventory(); i++)
            {
                if (inv.getStackInSlot(i) == null) continue;
                ItemStack sis = inv.getStackInSlot(i);
                if (sis.getItem() == is.getItem() && ItemStack.areItemStackTagsEqual(sis, is))
                {
                    return ctx.player.openContainer.getSlotFromInventory(inv, i);
                }
            }
        }
        return null;
    }

    public Multiset<ItemStackHolder> getInventoryContent(Action.ActionContext context)
    {
        int slotLow;
        int slotHigh;
        if (context.slot.inventory == context.player.inventory)
        {
            boolean isPlayerContainer = context.player.openContainer == context.player.inventoryContainer;
            boolean sourceHotBar = context.slot.getSlotIndex() < 9;
            int offset = isPlayerContainer ? 4 : 0;
            InventoryMapping m = context.mapping.get(context.player.inventory);
            slotLow = sourceHotBar ? m.end - 8 : m.begin + offset;
            slotHigh = sourceHotBar ? m.end + 1: m.end - 8;
        }
        else
        {
            InventoryMapping m = context.mapping.get(context.slot.inventory);
            slotLow = m.begin;
            slotHigh = m.end + 1;
        }
        SortedMultiset<ItemStackHolder> itemcounts = TreeMultiset.create(new InventoryHandler.ItemStackComparator());
        for (int i = slotLow; i < slotHigh; i++)
        {
            final Slot slot = context.player.openContainer.getSlot(i);
            if (!slot.canTakeStack(context.player)) continue;
            ItemStack stack = slot.getStack();
            if (stack != null && stack.getItem() != null)
            {
                ItemStackHolder ish = new ItemStackHolder(stack.copy());
                itemcounts.add(ish, stack.stackSize);
            }
        }

        return itemcounts;
    }

    public static class ItemStackComparator implements Comparator<ItemStackHolder>
    {

        @Override
        public int compare(ItemStackHolder o1, ItemStackHolder o2)
        {
            if (o1.is.getItem() != o2.is.getItem())
                return o1.is.getItem().getRegistryName().compareTo(o2.is.getItem().getRegistryName());
            if (o1.is.getMetadata() != o2.is.getMetadata())
                return Ints.compare(o1.is.getMetadata(), o2.is.getMetadata());
            if (ItemStack.areItemStackTagsEqual(o1.is, o2.is))
                return 0;
            return Ints.compare(System.identityHashCode(o1), System.identityHashCode(o2));
        }
    }
    public static class InventoryMapping
    {
        int begin = Integer.MAX_VALUE;
        int end = 0;
        final IInventory inv;
        final Container container;

        InventoryMapping(IInventory inv, Container container)
        {
            this.inv = inv;
            this.container = container;
        }

        @Override
        public String toString()
        {
            return Objects.toStringHelper(this).add("i", inv).add("c", container).add("b",begin).add("e",end).toString();
        }
    }
}
