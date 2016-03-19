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
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
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
        ItemStack is = InventoryHandler.INSTANCE.getItemStack(context);
        final Map<IInventory, InventoryHandler.InventoryMapping> mapping = context.mapping;
        if (is == null) return null;
        Slot source;
        if (moveAmount == -1)
        {
            source = InventoryHandler.INSTANCE.findStackWithItem(is, context, mapping, context.slot);
        }
        else
        {
            source = context.slot;
        }

        if (source == null) return null;

        if (!source.canTakeStack(context.player)) return null;
        if (!source.isItemValid(is)) return null;
        ItemStack sourceStack = InventoryHandler.INSTANCE.getItemStack(source);
        ItemStack iscopy = sourceStack.copy();
        iscopy.stackSize = 1;
        InventoryHandler.INSTANCE.moveItemToOtherInventory(source, context, mapping, iscopy, moveAmount != -1);
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
        }
        return null;
    }

}
