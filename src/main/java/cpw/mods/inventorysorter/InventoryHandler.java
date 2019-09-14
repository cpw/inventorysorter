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

import com.google.common.base.*;
import com.google.common.collect.*;
import com.google.common.primitives.*;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.*;
import net.minecraft.tileentity.*;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.*;
import java.util.*;

/**
 * @author cpw
 */
public enum InventoryHandler
{
    INSTANCE;
    public final Method mergeStack = getMergeStackMethod();

    private Method getMergeStackMethod()
    {

        Method m = ObfuscationReflectionHelper.findMethod(Container.class, "func_"+"75135_a", ItemStack.class, int.class, int.class, boolean.class);
        m.setAccessible(true);
        return m;
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

    public ItemStack getItemStack(ContainerContext ctx)
    {
        return getItemStack(ctx.slot);
    }

    public ItemStack getItemStack(Slot slot)
    {
        if (slot.getSlotIndex() < 0) return ItemStack.EMPTY;
        return slot.getStack();
    }

    public void moveItemToOtherInventory(ContainerContext ctx, ItemStack is, int targetLow, int targetHigh, boolean slotIsDestination)
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
            ContainerContext.PLAYER_HOTBAR, ImmutableList.of(ContainerContext.PLAYER_OFFHAND, ContainerContext.PLAYER_MAIN),
            ContainerContext.PLAYER_OFFHAND, ImmutableList.of(ContainerContext.PLAYER_HOTBAR, ContainerContext.PLAYER_MAIN),
            ContainerContext.PLAYER_MAIN, ImmutableList.of(ContainerContext.PLAYER_OFFHAND, ContainerContext.PLAYER_HOTBAR)
    );
    public Slot findStackWithItem(ItemStack is, final ContainerContext ctx)
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

    List<Map.Entry<IInventory, InventoryMapping>> getSortedMapping(final ContainerContext ctx)
    {
        List<Map.Entry<IInventory, InventoryMapping>> entries = Lists.newArrayList(ctx.mapping.entrySet());
        if (preferredOrders.containsKey(ctx.slotMapping.inv)) {
            Collections.sort(entries, (o1, o2) -> {
                int idx1 = preferredOrders.get(ctx.slotMapping.inv).indexOf(o1.getKey());
                int idx2 = preferredOrders.get(ctx.slotMapping.inv).indexOf(o2.getKey());
                return Ints.compare(idx1,idx2);
            });
        }
        return entries;
    }

    public Multiset<ItemStackHolder> getInventoryContent(ContainerContext context)
    {
        int slotLow = context.slotMapping.begin;
        int slotHigh = context.slotMapping.end + 1;
        SortedMultiset<ItemStackHolder> itemcounts = TreeMultiset.create(new ItemStackComparator());
        for (int i = slotLow; i < slotHigh; i++)
        {
            final Slot slot = context.player.openContainer.getSlot(i);
            if (!slot.canTakeStack(context.player)) continue;
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty())
            {
                ItemStackHolder ish = new ItemStackHolder(stack.copy());
                itemcounts.add(ish, stack.getCount());
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
            if (o1.is == o2.is) return 0;
            if (o1.is.getItem() != o2.is.getItem())
                return String.valueOf(o1.is.getItem().getRegistryName()).compareTo(String.valueOf(o2.is.getItem().getRegistryName()));
            if (ItemStack.areItemStackTagsEqual(o1.is, o2.is))
                return 0;
            return Ints.compare(System.identityHashCode(o1.is), System.identityHashCode(o2.is));
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
        boolean markAsHeterogeneous;

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
            return MoreObjects.toStringHelper(this).add("i", inv).add("c", container).add("b",begin).add("e",end).toString();
        }

        void addSlot(final Slot sl) {
            if (this.slotType != sl.getClass() && !(this.inv instanceof PlayerInventory) && !(this.inv instanceof FurnaceTileEntity) && !(this.inv instanceof BrewingStandTileEntity)) {
                this.markForRemoval = true;
            }
            if (this.slotType != sl.getClass())
            {
                this.markAsHeterogeneous = true;
            }
            this.begin = Math.min(sl.slotNumber, this.begin);
            this.end = Math.max(sl.slotNumber, this.end);

        }
    }
}
