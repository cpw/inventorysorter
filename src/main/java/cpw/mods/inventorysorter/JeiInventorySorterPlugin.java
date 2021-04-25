package cpw.mods.inventorysorter;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.api.gui.ingredient.IGuiIngredient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JeiPlugin
public class JeiInventorySorterPlugin implements IModPlugin {
    private static final ResourceLocation ID = new ResourceLocation("inventorysorter");

    @Nullable
    public static IIngredientManager ingredientManager;

    @Nullable
    public static IJeiRuntime jeiRuntime;

    @Nullable
    public static Collection<?> itemList;

    @Nullable
    public static Collection<IIngredientType<?>> ingredientTypes;

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        System.out.println("onRuntimeAvailable");
        JeiInventorySorterPlugin.jeiRuntime = jeiRuntime;
        JeiInventorySorterPlugin.ingredientManager = jeiRuntime.getIngredientManager();

//        Collection<IIngredientType> ingredientTypes = ingredientManager.getRegisteredIngredientTypes();

        Collection<?> allIngredients = new ArrayList<>();

        ingredientTypes = ingredientManager.getRegisteredIngredientTypes();

        for (IIngredientType<?> ingredientType : ingredientTypes) {
            Collection<?> ingredientList = ingredientManager.getAllIngredients(ingredientType);

            allIngredients = Stream.concat(allIngredients.stream(), ingredientList.stream()).collect(Collectors.toList());
        }

        JeiInventorySorterPlugin.itemList = allIngredients;
    }

    @Nonnull
    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }
}
