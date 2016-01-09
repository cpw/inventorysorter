package cpw.mods.inventorysorter;

import com.google.common.base.Function;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by cpw on 08/01/16.
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
        ItemStack is = InventoryHandler.INSTANCE.getItemStack(context);
        final Map<IInventory, InventoryHandler.InventoryMapping> mapping = context.mapping;

        System.out.printf("IS %s\n", is);
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

        System.out.printf("Source slot : %s %s %d\n", source, source != null ? source.inventory : null, source != null ? source.slotNumber : -1);
        if (source == null) return null;

        ItemStack sourceStack = InventoryHandler.INSTANCE.getItemStack(source);
        ItemStack iscopy = sourceStack.copy();
        iscopy.stackSize = 1;
        System.out.printf("IS %s %s\n", sourceStack, iscopy);
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
