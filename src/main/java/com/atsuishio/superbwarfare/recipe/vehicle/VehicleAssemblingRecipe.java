package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.init.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class VehicleAssemblingRecipe implements Recipe<Inventory> {

    private final ResourceLocation id;
    private final Category category;
    private final VehicleAssemblingResult result;
    private final List<VehicleAssemblingIngredient> inputs;

    public VehicleAssemblingRecipe(ResourceLocation id, VehicleAssemblingRecipeData data) {
        this(id, Category.getCategory(data.getCategory()), data.getResult(), data.getInputs());
    }

    public VehicleAssemblingRecipe(ResourceLocation id, Category recipeCategory, VehicleAssemblingResult result, List<VehicleAssemblingIngredient> inputs) {
        this.id = id;
        this.category = recipeCategory;
        this.result = result;
        this.inputs = inputs;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean matches(Inventory pContainer, Level pLevel) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack assemble(Inventory pContainer, RegistryAccess pRegistryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess pRegistryAccess) {
        return this.result.getResult().copy();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return this.id;
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
        AIRCRAFT("aircraft"),
        CIVILIAN("civilian"),
        DEFENSE("defense"),
        LAND("land"),
        WATER("water"),
        EMPTY("empty");

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
            return Category.EMPTY;
        }
    }
}
