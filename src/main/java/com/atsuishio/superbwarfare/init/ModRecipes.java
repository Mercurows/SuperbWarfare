package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.recipe.AmmoBoxAddAmmoRecipe;
import com.atsuishio.superbwarfare.recipe.AmmoBoxExtractAmmoRecipe;
import com.atsuishio.superbwarfare.recipe.PotionMortarShellRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Mod.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PotionMortarShellRecipe>> POTION_MORTAR_SHELL_SERIALIZER =
            RECIPE_SERIALIZERS.register("potion_mortar_shell", () -> new SimpleCraftingRecipeSerializer<>(PotionMortarShellRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoBoxAddAmmoRecipe>> AMMO_BOX_ADD_AMMO_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammo_box_add_ammo", () -> new SimpleCraftingRecipeSerializer<>(AmmoBoxAddAmmoRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<AmmoBoxExtractAmmoRecipe>> AMMO_BOX_EXTRACT_AMMO_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammo_box_extract_ammo", () -> new SimpleCraftingRecipeSerializer<>(AmmoBoxExtractAmmoRecipe::new));
}
