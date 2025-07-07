package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.recipe.AmmoBoxAddAmmoRecipe;
import com.atsuishio.superbwarfare.recipe.AmmoBoxExtractAmmoRecipe;
import com.atsuishio.superbwarfare.recipe.PotionMortarShellRecipe;
import com.atsuishio.superbwarfare.recipe.SmokeDyeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Mod.MODID);

    public static final RegistryObject<RecipeSerializer<PotionMortarShellRecipe>> POTION_MORTAR_SHELL_SERIALIZER =
            RECIPE_SERIALIZERS.register("potion_mortar_shell", () -> new SimpleCraftingRecipeSerializer<>(PotionMortarShellRecipe::new));

    public static final RegistryObject<RecipeSerializer<AmmoBoxAddAmmoRecipe>> AMMO_BOX_ADD_AMMO_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammo_box_add_ammo", () -> new SimpleCraftingRecipeSerializer<>(AmmoBoxAddAmmoRecipe::new));
    public static final RegistryObject<RecipeSerializer<AmmoBoxExtractAmmoRecipe>> AMMO_BOX_EXTRACT_AMMO_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammo_box_extract_ammo", () -> new SimpleCraftingRecipeSerializer<>(AmmoBoxExtractAmmoRecipe::new));

    public static final RegistryObject<RecipeSerializer<SmokeDyeRecipe>> SMOKE_DYE_SERIALIZER =
            RECIPE_SERIALIZERS.register("smoke_dye", () -> new SimpleCraftingRecipeSerializer<>(SmokeDyeRecipe::new));
}
