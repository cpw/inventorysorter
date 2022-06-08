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
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.*;

import javax.annotation.*;
import java.util.function.*;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * @author cpw
 */
public enum SortingHandler implements Consumer<ContainerContext>
{
    INSTANCE;
    @Override
    public void accept(ContainerContext context)
    {
        if (context == null) throw new NullPointerException("WHUT");
        // Ignore if we can't find ourselves in the slot set
        if (context.slotMapping == null) return;
        final Multiset<ItemStackHolder> itemcounts = InventoryHandler.INSTANCE.getInventoryContent(context);

        if (context.slot.container instanceof CraftingContainer)
        {
            distributeInventory(context, itemcounts);
        }
        else if (!context.slotMapping.markAsHeterogeneous)
        {
            compactInventory(context, itemcounts);
        }
    }

    private static ItemStack getStackInRowAndColumn(CraftingContainer inventory, int x, int y) {
        return inventory.getItem(y * inventory.getWidth() + x);
    }

    private void distributeInventory(final ContainerContext context, final Multiset<ItemStackHolder> itemcounts)
    {
        CraftingContainer ic = (CraftingContainer) context.slot.container;
        Multiset<ItemStackHolder> slotCounts = TreeMultiset.create(new InventoryHandler.ItemStackComparator());
        for (int x=0; x<ic.getWidth(); x++)
        {
            for (int y=0; y<ic.getHeight(); y++)
            {
                ItemStack is = getStackInRowAndColumn(ic, x, y);
                if (!is.isEmpty())
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
                ItemStack is = getStackInRowAndColumn(ic, x, y);
                if (!is.isEmpty())
                {
                    ItemStackHolder ish = new ItemStackHolder(is);
                    int count = staticcounts.count(ish);
                    int slotNum = slotCounts.count(ish);
                    final int occurrences = count / slotNum;
                    itemcounts.remove(ish, occurrences);
                    is.setCount(occurrences);
                }
            }
        }
        for (int x=0; x<ic.getWidth(); x++)
        {
            for (int y = 0; y < ic.getHeight(); y++)
            {
                ItemStack is = getStackInRowAndColumn(ic, x, y);
                if (!is.isEmpty())
                {
                    ItemStackHolder ish = new ItemStackHolder(is);
                    if (itemcounts.count(ish) > 0)
                    {
                        is.grow(itemcounts.setCount(ish,0));
                    }
                }
            }
        }
        for (int slot = context.slotMapping.begin; slot < context.slotMapping.end + 1; slot++)
        {
            context.player.containerMenu.getSlot(slot).setChanged();
        }
    }
    private void compactInventory(final ContainerContext context, final Multiset<ItemStackHolder> itemcounts)
    {
        final ResourceLocation containerTypeName = lookupContainerTypeName(context.player.inventoryMenu);
        InventorySorter.INSTANCE.lastContainerType = containerTypeName;
        if (InventorySorter.INSTANCE.containerblacklist.contains(containerTypeName)) {
            InventorySorter.INSTANCE.debugLog("Container {} blacklisted", ()->new String[] {containerTypeName.toString()});
            return;
        }

        InventorySorter.INSTANCE.debugLog("Container \"{}\" being sorted", ()->new String[] {containerTypeName.toString()});
        final UnmodifiableIterator<Multiset.Entry<ItemStackHolder>> itemsIterator;
        try
        {
            itemsIterator = Multisets.copyHighestCountFirst(itemcounts).entrySet().iterator();
        }
        catch (Exception e)
        {
            InventorySorter.LOGGER.warn("Weird, the sorting didn't quite work!", e);
            return;
        }
        int slotLow = context.slotMapping.begin;
        int slotHigh = context.slotMapping.end + 1;

        Multiset.Entry<ItemStackHolder> stackHolder = itemsIterator.hasNext() ? itemsIterator.next() : null;
        int itemCount = stackHolder != null ? stackHolder.getCount() : 0;
        for (int i = slotLow; i < slotHigh; i++)
        {
            final Slot slot = context.player.containerMenu.getSlot(i);
            if (!slot.mayPickup(context.player) && slot.hasItem()) {
                InventorySorter.LOGGER.log(Level.DEBUG, "Slot {} of container {} disallows canTakeStack", ()->slot.index, ()-> containerTypeName);
                continue;
            }
            slot.set(ItemStack.EMPTY);
            ItemStack target = ItemStack.EMPTY;
            if (itemCount > 0 && stackHolder != null)
            {
                target = stackHolder.getElement().is.copy();
                target.setCount(Math.min(itemCount, target.getMaxStackSize()));
            }
            // The item isn't valid for this slot
            if (!target.isEmpty() && !slot.mayPlace(target)) {
                final ItemStack trg = target;
                InventorySorter.LOGGER.log(Level.DEBUG, "Item {} is not valid in slot {} of container {}", ()->trg, ()->slot.index, ()-> containerTypeName);
                continue;
            }
            slot.set(target.isEmpty() ? ItemStack.EMPTY : target);
            itemCount -= !target.isEmpty() ? target.getCount() : 0;
            if (itemCount == 0)
            {
                stackHolder = itemsIterator.hasNext() ? itemsIterator.next() : null;
                itemCount = stackHolder != null ? stackHolder.getCount() : 0;
            }
        }
    }

    private static final ResourceLocation DUMMY_PLAYER_CONTAINER = new ResourceLocation("inventorysorter:dummyplayercontainer");
    private ResourceLocation lookupContainerTypeName(AbstractContainerMenu container) {
        return container instanceof InventoryMenu ? DUMMY_PLAYER_CONTAINER : ForgeRegistries.CONTAINERS.getKey(container.getType());
    }
}
