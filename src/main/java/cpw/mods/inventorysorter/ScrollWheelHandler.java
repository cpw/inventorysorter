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
import com.google.common.collect.Lists;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author cpw
 */
public enum ScrollWheelHandler implements Function<Action.ActionContext, Void>
{
    ONEITEMIN(-1), ONEITEMOUT(1);

    private final int moveAmount;

    ScrollWheelHandler(int amount)
    {
        this.moveAmount = amount;
    }
    @Nullable
    @Override
    public Void apply(@Nullable Action.ActionContext context)
    {
        if (context == null) throw new NullPointerException("WHUT");
        // Skip if we can't find ourselves in the mapping table
        if (context.slotMapping == null) return null;
        ItemStack is = InventoryHandler.INSTANCE.getItemStack(context);
        if (is == null) return null;
        final Map<IInventory, InventoryHandler.InventoryMapping> mapping = context.mapping;
        Slot source;
        if (moveAmount < 0 && is.getMaxStackSize() > is.stackSize)
        {
            source = InventoryHandler.INSTANCE.findStackWithItem(is, context);
        }
        else if (moveAmount > 0)
        {
            source = context.slot;
        }
        else
        {
            return null;
        }

        if (source == null) return null;

        if (!source.canTakeStack(context.player)) return null;
        if (!source.isItemValid(is)) return null;
        ItemStack sourceStack = InventoryHandler.INSTANCE.getItemStack(source);
        if (sourceStack == null) return null; // null detection
        ItemStack iscopy = sourceStack.copy();
        iscopy.stackSize = 1;

        List<InventoryHandler.InventoryMapping> mappingCandidates = Lists.newArrayList();
        if (moveAmount < 0)
        {
            final InventoryHandler.InventoryMapping inventoryMapping = new InventoryHandler.InventoryMapping(context.slot.inventory, context.player.openContainer);
            mappingCandidates.add(inventoryMapping);
            inventoryMapping.begin = context.slot.slotNumber;
            inventoryMapping.end = context.slot.slotNumber;
        }
        else
        {
            if (context.player.openContainer == context.player.inventoryContainer)
            {
                if (InventoryHandler.preferredOrders.containsKey(context.slotMapping.inv)) {
                    mappingCandidates.addAll(Lists.transform(InventoryHandler.preferredOrders.get(context.slotMapping.inv),new Function<IInventory, InventoryHandler.InventoryMapping>()
                    {
                        @Nullable
                        @Override
                        public InventoryHandler.InventoryMapping apply(@Nullable IInventory input)
                        {
                            return mapping.get(input);
                        }
                    }));
                }
                Collections.reverse(mappingCandidates);
            }
            else
            {
                for (Map.Entry<IInventory, InventoryHandler.InventoryMapping> entry : InventoryHandler.INSTANCE.getSortedMapping(context))
                {
                    if (entry.getValue().proxy == context.slot.inventory) continue;
                    mappingCandidates.add(entry.getValue());
                }
            }
        }
        Collections.reverse(mappingCandidates);
        for (InventoryHandler.InventoryMapping mappingCandidate : mappingCandidates)
        {
            if (mappingCandidate.inv == Action.ActionContext.PLAYER_OFFHAND && moveAmount > 0) {
                boolean empty = true;
                for (ItemStack itemStack : context.player.inventory.offHandInventory)
                {
                    if (itemStack != null) empty = false;
                }
                if (empty) continue;
            }
            if (mappingCandidate.inv == Action.ActionContext.PLAYER_HOTBAR && moveAmount > 0) {
                boolean hasTarget = false, found = false;
                for (int i = 0; i < 9; i++)
                {
                    ItemStack itemStack = context.player.inventory.mainInventory[i];
                    if (ItemStack.areItemsEqual(itemStack,sourceStack) && itemStack.stackSize < itemStack.getMaxStackSize())
                    {
                        hasTarget = true;
                    }
                    else if (ItemStack.areItemsEqual(itemStack,sourceStack))
                    {
                        found = true;
                    }
                }
                if (!hasTarget && found) continue;
            }
            InventoryHandler.INSTANCE.moveItemToOtherInventory(context, iscopy, mappingCandidate.begin, mappingCandidate.end+1, moveAmount < 0);
            if (iscopy.stackSize == 0)
            {
                sourceStack.stackSize--;
                if (sourceStack.stackSize == 0)
                {
                    source.putStack(null);
                }
                else
                {
                    source.onSlotChanged();
                }
                break;
            }
        }
        return null;
    }

}
