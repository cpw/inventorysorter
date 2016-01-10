package cpw.mods.inventorysorter;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.JsonUtils;
import scala.swing.ListView;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
        IInventory inv = context.slot.inventory;
        final Multiset<ItemStackHolder> itemcounts = InventoryHandler.INSTANCE.getInventoryContent(context);
        final UnmodifiableIterator<Multiset.Entry<ItemStackHolder>> itemsIterator = Multisets.copyHighestCountFirst(itemcounts).entrySet().iterator();
        int slotLow = 0;
        int slotHigh = 0;
        if (inv == context.player.inventory)
        {
            boolean sourceHotBar = context.slot.getSlotIndex() < 9;
            InventoryHandler.InventoryMapping m = context.mapping.get(context.player.inventory);
            slotLow = sourceHotBar ? m.end - 8 : m.begin + 4;
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
            if (itemCount > 0)
            {
                target = stackHolder.getElement().is.copy();
                target.stackSize = itemCount > target.getMaxStackSize() ? target.getMaxStackSize() : itemCount;
            }
            if (!slot.isItemValid(target) || !slot.canTakeStack(context.player)) continue;
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
