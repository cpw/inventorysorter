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
        System.out.printf("IS: %s %s\n", stack, stack.getTagCompound());
    }

    @Override
    public int hashCode()
    {
        return is.getItem().hashCode() * 31 + (is.getMetadata() * 31 * 31 ) + (is.hasTagCompound() ? is.getTagCompound().hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof ItemStackHolder)) return false;
        ItemStackHolder ish = (ItemStackHolder)obj;
        System.out.println("="+is.getTagCompound()+" "+ish.is.getTagCompound());
        return is.getItem() == ish.is.getItem() && is.getMetadata() == ish.is.getMetadata() && ItemStack.areItemStackTagsEqual(is, ish.is);
    }
}
