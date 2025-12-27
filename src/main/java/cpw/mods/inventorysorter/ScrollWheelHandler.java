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

import net.minecraft.world.inventory.Slot;

import java.util.*;
import java.util.function.*;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * @author cpw
 */
public enum ScrollWheelHandler implements Consumer<ContainerContext>
{
    ONEITEMIN(-1), ONEITEMOUT(1);

    private final int moveAmount;

    ScrollWheelHandler(int amount)
    {
        this.moveAmount = amount;
    }

    @Override
    public void accept(@SuppressWarnings("ClassEscapesDefinedScope") ContainerContext context)
    {
        if (context == null) throw new NullPointerException("WHUT");
        // Skip if we can't find ourselves in the mapping table
        if (context.slotMapping == null) return;
        ItemStack is = InventoryHandler.INSTANCE.getItemStack(context);
        if (is == null) return;
        final Map<Container, InventoryHandler.InventoryMapping> mapping = context.mapping;
        Slot source;
        if (moveAmount < 0 && is.getMaxStackSize() > is.getCount())
        {
            source = InventoryHandler.INSTANCE.findStackWithItem(is, context);
        }
        else if (moveAmount > 0)
        {
            source = context.slot;
        }
        else
        {
            return;
        }

        if (source == null) return;

        if (InventorySorter.INSTANCE.isContainerBlacklisted(InventoryHandler.lookupContainerTypeName(context.slotMapping.container))) return; // Blacklist container screen
        if (InventorySorter.INSTANCE.isSlotBlacklisted(source)) return; // Blacklist source
        if (InventorySorter.INSTANCE.isSlotBlacklisted(context.slot)) return; // Blacklist target

        if (!source.mayPickup(context.player)) return;
        if (!source.mayPlace(is)) return;
        final ItemStack sourceStack = InventoryHandler.INSTANCE.getItemStack(source);
        if (sourceStack.isEmpty()) return; // emptystack detection
        ItemStack iscopy = sourceStack.copy();
        iscopy.setCount(1);

        final List<InventoryHandler.InventoryMapping> mappingCandidates = new ArrayList<>();
        if (moveAmount < 0)
        {
            final InventoryHandler.InventoryMapping inventoryMapping = new InventoryHandler.InventoryMapping(context.slot.container, context.player.containerMenu, context.slot.container, context.slot);
            mappingCandidates.add(inventoryMapping);
            inventoryMapping.begin = context.slot.index;
            inventoryMapping.end = context.slot.index;
        }
        else
        {
            if (context.player.containerMenu == context.player.inventoryMenu)
            {
                if (InventoryHandler.preferredOrders.containsKey(context.slotMapping.inv)) {
                    mappingCandidates.addAll(InventoryHandler.preferredOrders.get(context.slotMapping.inv).stream().map(mapping::get).toList());
                }
                Collections.reverse(mappingCandidates);
            }
            else
            {
                for (Map.Entry<Container, InventoryHandler.InventoryMapping> entry : InventoryHandler.INSTANCE.getSortedMapping(context))
                {
                    if (entry.getValue().proxy == context.slot.container) continue;
                    if (InventorySorter.INSTANCE.isSlotBlacklisted(entry.getValue().slot)) continue; //remove blacklisted
                    mappingCandidates.add(entry.getValue());
                }
            }
        }
        Collections.reverse(mappingCandidates);
        for (InventoryHandler.InventoryMapping mappingCandidate : mappingCandidates)
        {
            if (mappingCandidate.inv == ContainerContext.PLAYER_OFFHAND && moveAmount > 0) {
                var itemStack = context.player.getOffhandItem();
                if (itemStack.isEmpty()) {
                    continue;
                }
            }
            if (mappingCandidate.inv == ContainerContext.PLAYER_HOTBAR && moveAmount > 0) {
                boolean hasTarget = false, found = false;
                for (int i = 0; i < 9; i++)
                {
                    ItemStack itemStack = context.player.getInventory().getItem(i);
                    if (ItemStack.isSameItem(itemStack,sourceStack) && itemStack.getCount() < itemStack.getMaxStackSize())
                    {
                        hasTarget = true;
                    }
                    else if (ItemStack.isSameItem(itemStack,sourceStack))
                    {
                        found = true;
                    }
                }
                if (!hasTarget && found) continue;
            }

            InventoryHandler.INSTANCE.moveItemToOtherInventory(context, iscopy, mappingCandidate.begin, mappingCandidate.end+1, moveAmount < 0);
            if (iscopy.getCount() == 0)
            {
                sourceStack.shrink(1);
                source.set(sourceStack);
                break;
            }
        }
    }

}
