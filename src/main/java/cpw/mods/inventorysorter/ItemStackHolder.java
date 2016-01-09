package cpw.mods.inventorysorter;

import net.minecraft.item.ItemStack;

/**
 * Created by cpw on 08/01/16.
 */
public class ItemStackHolder
{
    public final ItemStack is;

    public ItemStackHolder(ItemStack stack)
    {
        this.is = stack;
    }

    @Override
    public int hashCode()
    {
        return is.getItem().hashCode() * 31 + (is.hasTagCompound() ? is.getTagCompound().hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof ItemStackHolder)) return false;
        if (is.getMaxStackSize() == 1) return false;
        ItemStackHolder ish = (ItemStackHolder)obj;
        return is.getItem() == ish.is.getItem() && ItemStack.areItemStackTagsEqual(is, ish.is);
    }
}
