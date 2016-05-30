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
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author cpw
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
        if (slot.getSlotIndex() < 0) return null;
        return slot.inventory.getStackInSlot(slot.getSlotIndex());
    }

    public void moveItemToOtherInventory(Action.ActionContext ctx, ItemStack is, int targetLow, int targetHigh, boolean slotIsDestination)
    {
        for (int i = targetLow; i < targetHigh; i++)
        {
            if (!ctx.player.openContainer.getSlot(i).isItemValid(is))
            {
                continue;
            }
            if (mergeStack(ctx.player.openContainer, is, i, i+1, slotIsDestination))
            {
                break;
            }
        }
    }

    static Map<IInventory,ImmutableList<IInventory>> preferredOrders = ImmutableMap.of(
            Action.ActionContext.PLAYER_HOTBAR, ImmutableList.of(Action.ActionContext.PLAYER_OFFHAND, Action.ActionContext.PLAYER_MAIN),
            Action.ActionContext.PLAYER_OFFHAND, ImmutableList.of(Action.ActionContext.PLAYER_HOTBAR, Action.ActionContext.PLAYER_MAIN),
            Action.ActionContext.PLAYER_MAIN, ImmutableList.of(Action.ActionContext.PLAYER_OFFHAND, Action.ActionContext.PLAYER_HOTBAR)
    );
    public Slot findStackWithItem(ItemStack is, final Action.ActionContext ctx)
    {
        if (is.getMaxStackSize() == 1) return null;

        List<Map.Entry<IInventory, InventoryMapping>> entries = getSortedMapping(ctx);
        for (Map.Entry<IInventory, InventoryMapping> ent : entries)
        {
            IInventory inv = ent.getKey();
            if (inv == ctx.slotMapping.inv) continue;
            for (int i = ent.getValue().begin; i <= ent.getValue().end; i++)
            {
                final Slot slot = ctx.player.openContainer.getSlot(i);
                if (!slot.canTakeStack(ctx.player)) continue;
                ItemStack sis = slot.getStack();
                if (sis != null && sis.getItem() == is.getItem() && ItemStack.areItemStackTagsEqual(sis, is))
                {
                    return slot;
                }
            }
        }
        return null;
    }

    List<Map.Entry<IInventory, InventoryMapping>> getSortedMapping(final Action.ActionContext ctx)
    {
        List<Map.Entry<IInventory, InventoryMapping>> entries = Lists.newArrayList(ctx.mapping.entrySet());
        if (preferredOrders.containsKey(ctx.slotMapping.inv)) {
            Collections.sort(entries, new Comparator<Map.Entry<IInventory, InventoryMapping>>()
            {
                public int compare(Map.Entry<IInventory, InventoryMapping> o1, Map.Entry<IInventory, InventoryMapping> o2)
                {
                    int idx1 = preferredOrders.get(ctx.slotMapping.inv).indexOf(o1.getKey());
                    int idx2 = preferredOrders.get(ctx.slotMapping.inv).indexOf(o2.getKey());
                    return Ints.compare(idx1,idx2);
                }
            });
        }
        return entries;
    }

    public Multiset<ItemStackHolder> getInventoryContent(Action.ActionContext context)
    {
        int slotLow = context.slotMapping.begin;
        int slotHigh = context.slotMapping.end + 1;
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
        final HashMultiset<ItemStackHolder> entries = HashMultiset.create();
        for (Multiset.Entry<ItemStackHolder> entry : itemcounts.descendingMultiset().entrySet())
        {
            entries.add(entry.getElement(),entry.getCount());
        }
        return entries;
    }

    public static class ItemStackComparator implements Comparator<ItemStackHolder>
    {

        @Override
        public int compare(ItemStackHolder o1, ItemStackHolder o2)
        {
            if (o1 == o2) return 0;
            if (o1.is.getItem() != o2.is.getItem())
                return o1.is.getItem().getRegistryName().toString().compareTo(o2.is.getItem().getRegistryName().toString());
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
        final IInventory proxy;
        final Container container;
        final Class<? extends Slot> slotType;
        boolean markForRemoval;

        InventoryMapping(IInventory inv, Container container, IInventory proxy, Class<? extends Slot> slotType)
        {
            this.inv = inv;
            this.container = container;
            this.proxy = proxy;
            this.slotType = slotType;
        }
        @Override
        public String toString()
        {
            return Objects.toStringHelper(this).add("i", inv).add("c", container).add("b",begin).add("e",end).toString();
        }
    }
}
