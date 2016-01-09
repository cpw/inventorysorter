package cpw.mods.inventorysorter;

import com.google.common.base.Function;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import scala.swing.ListView;

import javax.annotation.Nullable;
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
        int invSize = inv.getSizeInventory();
        Multiset<ItemStackHolder> itemcounts = LinkedHashMultiset.create();
        for (int i = 0; i < invSize; i++)
        {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() != null)
            {
                ItemStackHolder ish = new ItemStackHolder(stack);
                itemcounts.add(ish, ish.is.stackSize);
            }
        }

        for (Multiset.Entry<ItemStackHolder> ish : Multisets.copyHighestCountFirst(itemcounts).entrySet())
        {
            int count = ish.getCount();
            ItemStackHolder itemStackHolder = ish.getElement();
            int stacks = count / itemStackHolder.is.getMaxStackSize();
            System.out.printf("Sorting %s %s for (%d) %d stacks %d leftover\n", itemStackHolder, itemStackHolder.is, count, stacks, count % itemStackHolder.is.getMaxStackSize());
        }

        return null;
    }
}
