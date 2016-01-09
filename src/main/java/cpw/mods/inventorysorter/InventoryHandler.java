package cpw.mods.inventorysorter;

import com.google.common.collect.Maps;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * Created by cpw on 09/01/16.
 */
public enum InventoryHandler
{
    INSTANCE;
    public final Method mergeStack = getMergeStackMethod();

    private Method getMergeStackMethod()
    {
        try
        {
            Method m = Container.class.getDeclaredMethod("mergeItemStack", ItemStack.class, int.class, int.class, boolean.class);
            m.setAccessible(true);
            return m;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public void mergeStack(Container container, ItemStack stack, int low, int high, boolean rev)
    {
        try
        {
            mergeStack.invoke(container, stack, low, high, rev);
        } catch (Exception e)
        {

        }
    }

    public ItemStack getItemStack(Action.ActionContext ctx)
    {
        return getItemStack(ctx.slot);
    }

    public ItemStack getItemStack(Slot slot)
    {
        return slot.inventory.getStackInSlot(slot.getSlotIndex());
    }

    public void moveItemToOtherInventory(Slot origin, Action.ActionContext ctx, Map<IInventory,InventoryMapping> mapping, ItemStack is)
    {
        int targetLow = 0;
        int targetHigh = 0;

        if (ctx.player.inventoryContainer == ctx.player.openContainer)
        {
            boolean sourceHotBar = origin.slotNumber >= 36 && origin.slotNumber < 45;
            targetLow = sourceHotBar ? 9 : 36;
            targetHigh = sourceHotBar ? 36 : 45;
        }
        else if (origin.inventory == ctx.player.inventory)
        {
            for (Map.Entry<IInventory, InventoryMapping> m : mapping.entrySet())
            {
                if (m.getKey() != ctx.player.inventory)
                {
                    targetLow = m.getValue().begin;
                    targetHigh = m.getValue().end;
                }
            }
        }
        else
        {
            InventoryMapping m = mapping.get(ctx.player.inventory);
            targetLow = m.begin;
            targetHigh = m.end;
        }
        mergeStack(ctx.player.openContainer, is, targetLow, targetHigh, false);
    }

    public Slot findStackWithItem(ItemStack is, Action.ActionContext ctx, Map<IInventory,InventoryMapping> mapping, Slot origin)
    {
        if (is.getMaxStackSize() == 1) return null;

        if (ctx.player.inventoryContainer == ctx.player.openContainer)
        {
            boolean sourceHotBar = origin.slotNumber < 9;
            int searchLow = sourceHotBar ? 9 : 36;
            int searchHigh = sourceHotBar ? 36 : 45;
            for (int i = searchLow; i < searchHigh; i++)
            {
                ItemStack sis = ctx.player.inventoryContainer.getSlot(i).getStack();
                if (sis != null && sis.getItem() == is.getItem() && ItemStack.areItemStackTagsEqual(sis, is))
                {
                    return ctx.player.openContainer.getSlot(i);
                }
            }
            return null;
        }
        for (Map.Entry<IInventory, InventoryMapping> ent : ctx.mapping.entrySet())
        {
            IInventory inv = ent.getKey();
            if (inv == origin.inventory) continue;
            for (int i = 0; i < inv.getSizeInventory(); i++)
            {
                if (inv.getStackInSlot(i) == null) continue;
                ItemStack sis = inv.getStackInSlot(i);
                if (sis.getItem() == is.getItem() && ItemStack.areItemStackTagsEqual(sis, is))
                {
                    return ctx.player.openContainer.getSlotFromInventory(inv, i);
                }
            }
        }
        return null;
    }

    public static class InventoryMapping
    {
        int begin = Integer.MAX_VALUE;
        int end = 0;
        final IInventory inv;
        final Container container;

        InventoryMapping(IInventory inv, Container container)
        {
            this.inv = inv;
            this.container = container;
        }
    }
}
