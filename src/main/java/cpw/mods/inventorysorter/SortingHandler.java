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

import com.google.common.collect.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.apache.logging.log4j.*;

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
    public void accept(@SuppressWarnings("ClassEscapesDefinedScope") ContainerContext context)
    {
        if (context == null) throw new NullPointerException("WHUT");
        // Ignore if we can't find ourselves in the slot set
        if (context.slotMapping == null) return;
        final Multiset<ItemStackHolder> itemCounts = InventoryHandler.INSTANCE.getInventoryContent(context);

        // TODO: Allow other mods to mark their containers as crafting-like so they can benefit from distributed sorting
        if (context.slot.container instanceof CraftingContainer)
        {
            distributeInventory(context, itemCounts);
        }
        else if (!context.slotMapping.markAsHeterogeneous)
        {
            compactInventory(context, itemCounts);
        }
    }

    private static ItemStack getStackInRowAndColumn(CraftingContainer inventory, int x, int y) {
        return inventory.getItem(y * inventory.getWidth() + x);
    }

    /**
     * When sorting inside a crafting grid, we need to modify the sorting system to instead distribute
     * the items evenly across the grid rather than compacting them all to one side.
     */
    private void distributeInventory(final ContainerContext context, final Multiset<ItemStackHolder> itemCounts) {
        CraftingContainer inventoryContainer = (CraftingContainer) context.slot.container;
        Multiset<ItemStackHolder> slotCounts = TreeMultiset.create(new InventoryHandler.ItemStackComparator());
        for (int x=0; x<inventoryContainer.getWidth(); x++) {
            for (int y=0; y<inventoryContainer.getHeight(); y++) {
                ItemStack is = getStackInRowAndColumn(inventoryContainer, x, y);
                if (!is.isEmpty()) {
                    slotCounts.add(new ItemStackHolder(is));
                }
            }
        }

        final ImmutableMultiset<ItemStackHolder> staticCounts = ImmutableMultiset.copyOf(itemCounts);
        for (int x=0; x<inventoryContainer.getWidth(); x++) {
            for (int y = 0; y < inventoryContainer.getHeight(); y++) {
                ItemStack is = getStackInRowAndColumn(inventoryContainer, x, y);
                if (!is.isEmpty()) {
                    ItemStackHolder ish = new ItemStackHolder(is);
                    int count = staticCounts.count(ish);
                    int slotNum = slotCounts.count(ish);
                    final int occurrences = count / slotNum;
                    itemCounts.remove(ish, occurrences);
                    is.setCount(occurrences);
                }
            }
        }

        for (int x=0; x<inventoryContainer.getWidth(); x++) {
            for (int y = 0; y < inventoryContainer.getHeight(); y++) {
                ItemStack is = getStackInRowAndColumn(inventoryContainer, x, y);
                if (!is.isEmpty()) {
                    ItemStackHolder ish = new ItemStackHolder(is);
                    if (itemCounts.count(ish) > 0)
                    {
                        is.grow(itemCounts.setCount(ish,0));
                    }
                }
            }
        }

        for (int slot = context.slotMapping.begin; slot < context.slotMapping.end + 1; slot++) {
            context.player.containerMenu.getSlot(slot).setChanged();
        }
    }

    private void compactInventory(final ContainerContext context, final Multiset<ItemStackHolder> itemCounts) {
        final Identifier containerTypeName = InventoryHandler.lookupContainerTypeName(context.slotMapping.container);
        InventorySorter.INSTANCE.lastContainerType = containerTypeName;
        if (InventorySorter.INSTANCE.isContainerBlacklisted(containerTypeName)) {
            InventorySorter.INSTANCE.debugLog("Container {} blacklisted", ()->new String[] {containerTypeName.toString()});
            return;
        }

        InventorySorter.INSTANCE.debugLog("Container \"{}\" being sorted", ()->new String[] {containerTypeName.toString()});
        final UnmodifiableIterator<Multiset.Entry<ItemStackHolder>> itemsIterator;
        try {
            // TODO: Allow for configurable sorting orders here.
            itemsIterator = Multisets.copyHighestCountFirst(itemCounts).entrySet().iterator();
        } catch (Exception e) {
            InventorySorter.LOGGER.warn("Weird, the sorting didn't quite work!", e);
            return;
        }

        // TODO: Allow for mods to define safe start and end indexes to protect their "special" slots.
        // TODO: Allow for mods to provide an index of banned slots as well. This could be via id to complement the existing blacklist system.
        int slotLow = context.slotMapping.begin;
        int slotHigh = context.slotMapping.end + 1;

        Multiset.Entry<ItemStackHolder> stackHolder = itemsIterator.hasNext() ? itemsIterator.next() : null;
        int itemCount = stackHolder != null ? stackHolder.getCount() : 0;
        for (int i = slotLow; i < slotHigh; i++) {
            final Slot slot = context.player.containerMenu.getSlot(i);
            if (!slot.mayPickup(context.player) && slot.hasItem()) {
                InventorySorter.LOGGER.log(Level.DEBUG, "Slot {} of container {} disallows canTakeStack", ()->slot.index, ()-> containerTypeName);
                continue;
            }

            slot.set(ItemStack.EMPTY);
            ItemStack target = ItemStack.EMPTY;
            if (itemCount > 0 && stackHolder != null) {
                target = stackHolder.getElement().is().copy();
                target.setCount(Math.min(itemCount, slot.getMaxStackSize(target)));
            }

            // The item isn't valid for this slot
            if (!target.isEmpty() && !slot.mayPlace(target)) {
                final ItemStack trg = target;
                InventorySorter.LOGGER.log(Level.DEBUG, "Item {} is not valid in slot {} of container {}", ()->trg, ()->slot.index, ()-> containerTypeName);
                continue;
            }

            slot.set(target.isEmpty() ? ItemStack.EMPTY : target);
            itemCount -= !target.isEmpty() ? target.getCount() : 0;
            if (itemCount == 0) {
                stackHolder = itemsIterator.hasNext() ? itemsIterator.next() : null;
                itemCount = stackHolder != null ? stackHolder.getCount() : 0;
            }
        }
    }
}
