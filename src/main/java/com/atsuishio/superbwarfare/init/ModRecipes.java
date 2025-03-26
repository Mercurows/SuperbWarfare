package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.ModUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class ModRecipes {
    // TODO recipes
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, ModUtils.MODID);

//    public static final RegistryObject<RecipeSerializer<PotionMortarShellRecipe>> POTION_MORTAR_SHELL_SERIALIZER =
//            RECIPE_SERIALIZERS.register("potion_mortar_shell", () -> new SimpleCraftingRecipeSerializer<>(PotionMortarShellRecipe::new));
//
//    public static final RegistryObject<RecipeSerializer<AmmoBoxAddAmmoRecipe>> AMMO_BOX_ADD_AMMO_SERIALIZER =
//            RECIPE_SERIALIZERS.register("ammo_box_add_ammo", () -> new SimpleCraftingRecipeSerializer<>(AmmoBoxAddAmmoRecipe::new));
//    public static final RegistryObject<RecipeSerializer<AmmoBoxExtractAmmoRecipe>> AMMO_BOX_EXTRACT_AMMO_SERIALIZER =
//            RECIPE_SERIALIZERS.register("ammo_box_extract_ammo", () -> new SimpleCraftingRecipeSerializer<>(AmmoBoxExtractAmmoRecipe::new));

}
