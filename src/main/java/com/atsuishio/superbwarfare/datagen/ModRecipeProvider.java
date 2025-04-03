package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.recipe.AmmoBoxAddAmmoRecipe;
import com.atsuishio.superbwarfare.recipe.AmmoBoxExtractAmmoRecipe;
import com.atsuishio.superbwarfare.recipe.PotionMortarShellRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        SpecialRecipeBuilder.special(PotionMortarShellRecipe::new).save(recipeOutput, "potion_mortar_shell");
        SpecialRecipeBuilder.special(AmmoBoxAddAmmoRecipe::new).save(recipeOutput, "ammo_box_add_ammo");
        SpecialRecipeBuilder.special(AmmoBoxExtractAmmoRecipe::new).save(recipeOutput, "ammo_box_extract_ammo");
    }
}
