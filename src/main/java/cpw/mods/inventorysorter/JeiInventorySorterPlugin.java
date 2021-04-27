package cpw.mods.inventorysorter;

import com.google.common.collect.*;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@JeiPlugin
public class JeiInventorySorterPlugin implements IModPlugin {
    private static final ResourceLocation ID = new ResourceLocation("inventorysorter");

    @Nullable
    public static LinkedListMultimap<String, CompoundNBT> allIngredients;

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        if (ModList.get().isLoaded("jei")) {
            try {
                IIngredientManager ingredientManager = jeiRuntime.getIngredientManager();
                Collection<IIngredientType<?>> ingredientTypes = ingredientManager.getRegisteredIngredientTypes();

                List<Object> allIngredients = new ArrayList<Object>();

                for (IIngredientType<?> ingredientType : ingredientTypes) {
                    List<Object> currentIngredients = Arrays.asList(ingredientManager.getAllIngredients(ingredientType).toArray());

                    allIngredients.addAll(currentIngredients);
                }

                LinkedListMultimap<String, CompoundNBT> ingredientList = LinkedListMultimap.create();

                for (Object item : allIngredients) {
                    if (item instanceof ItemStack) {
                        ItemStack itemStack = (ItemStack) item;
                        String itemName = itemStack.getItem().getRegistryName().toString();
                        CompoundNBT tag = itemStack.getTag();

                        ingredientList.put(itemName, tag);
                    }
                }
                JeiInventorySorterPlugin.allIngredients = ingredientList;
            } catch (Exception e) {
                InventorySorter.LOGGER.warn("Something went wrong while initializing JeiInventorySorterPlugin.", e);
            }
        }
    }

    public static UnmodifiableIterator<Multiset.Entry<ItemStackHolder>> sortItems(Multiset<ItemStackHolder> itemcounts){
        List<Multiset.Entry<ItemStackHolder>> itemcountsList = Lists.newArrayList(itemcounts.entrySet());
        Collections.sort(itemcountsList, new JeiInventorySorterPlugin.ItemStackComparator());
        ImmutableMultiset<Multiset.Entry<ItemStackHolder>> itemsMultiset = ImmutableMultiset.copyOf(itemcountsList);
        return itemsMultiset.iterator();
    }

    private static class ItemStackComparator implements Comparator<Multiset.Entry<ItemStackHolder>> {
        @Override
        public int compare(Multiset.Entry<ItemStackHolder> first, Multiset.Entry<ItemStackHolder> second) {
            ItemStack firstItem = first.getElement().is;
            ItemStack secondItem = second.getElement().is;

            String firstItemName = firstItem.getItem().getRegistryName().toString();
            String secondItemName = secondItem.getItem().getRegistryName().toString();

            int firstNameIndex = Lists.newArrayList(allIngredients.keys()).indexOf(firstItemName);
            int secondNameIndex = Lists.newArrayList(allIngredients.keys()).indexOf(secondItemName);

            int itemNameTest = Integer.compare(firstNameIndex, secondNameIndex);

            if (itemNameTest != 0) {
                return itemNameTest;
            }

            CompoundNBT firstTag = firstItem.getTag();
            CompoundNBT secondTag = secondItem.getTag();

            List<CompoundNBT> tags = Lists.newArrayList(allIngredients.get(firstItemName));

            int firstTagIndex = tags.indexOf(firstTag);
            int secondTagIndex = tags.indexOf(secondTag);

            int itemTagTest = Integer.compare(firstTagIndex, secondTagIndex);

            return itemTagTest;
        }
    }

    @Nonnull
    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }
}
