package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VehicleAssemblingRecipe implements Recipe<RecipeWrapper> {

    private final Category category;
    private final VehicleAssemblingResult result;
    private final List<VehicleAssemblingIngredient> inputs;

    public VehicleAssemblingRecipe(List<VehicleAssemblingIngredient> inputs, String recipeCategory, VehicleAssemblingResult result) {
        this.category = Category.valueOf(recipeCategory);
        this.result = result;
        this.inputs = inputs;
    }

    public VehicleAssemblingRecipe(List<VehicleAssemblingIngredient> inputs, Category recipeCategory, VehicleAssemblingResult result) {
        this.category = recipeCategory;
        this.result = result;
        this.inputs = inputs;
    }

    public static VehicleAssemblingRecipe create(Map<String, Integer> ingredients, Category recipeCategory, EntityType<?> type) {
        List<VehicleAssemblingIngredient> inputs = new ArrayList<>();
        for (var entry : ingredients.entrySet()) {
            inputs.add(new VehicleAssemblingIngredient(entry.getKey(), entry.getValue()));
        }
        var result = new VehicleAssemblingResult(ModItems.CONTAINER.getId().toString(), BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), 1);
        return new VehicleAssemblingRecipe(inputs, recipeCategory, result);
    }

    public static VehicleAssemblingRecipe create(Map<String, Integer> ingredients, Category recipeCategory, Item result, int count) {
        List<VehicleAssemblingIngredient> inputs = new ArrayList<>();
        for (var entry : ingredients.entrySet()) {
            inputs.add(new VehicleAssemblingIngredient(entry.getKey(), entry.getValue()));
        }
        return new VehicleAssemblingRecipe(inputs, recipeCategory, new VehicleAssemblingResult(BuiltInRegistries.ITEM.getKey(result).toString(), "", count));
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean matches(RecipeWrapper pContainer, Level pLevel) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack assemble(RecipeWrapper recipeWrapper, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        return this.result.getResult().copy();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.VEHICLE_ASSEMBLING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.VEHICLE_ASSEMBLING_TYPE.get();
    }

    public Category getCategory() {
        return category;
    }

    public VehicleAssemblingResult getResult() {
        return result;
    }

    public List<VehicleAssemblingIngredient> getInputs() {
        return inputs;
    }

    public enum Category {
        LAND("land"),
        DEFENSE("defense"),
        AIRCRAFT("aircraft"),
        CIVILIAN("civilian"),
        WATER("water"),
        MISC("misc");

        private final String name;

        Category(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static Category getCategory(String name) {
            for (Category category : Category.values()) {
                if (category.getName().equals(name)) {
                    return category;
                }
            }
            return Category.MISC;
        }
    }
}
