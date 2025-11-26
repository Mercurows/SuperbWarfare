package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.recipe.*;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipeSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Mod.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Mod.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PotionMortarShellRecipe>> POTION_MORTAR_SHELL_SERIALIZER =
            RECIPE_SERIALIZERS.register("potion_mortar_shell", () -> new SimpleCraftingRecipeSerializer<>(PotionMortarShellRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoBoxAddAmmoRecipe>> AMMO_BOX_ADD_AMMO_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammo_box_add_ammo", () -> new SimpleCraftingRecipeSerializer<>(AmmoBoxAddAmmoRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoBoxExtractAmmoRecipe>> AMMO_BOX_EXTRACT_AMMO_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammo_box_extract_ammo", () -> new SimpleCraftingRecipeSerializer<>(AmmoBoxExtractAmmoRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SmokeDyeRecipe>> SMOKE_DYE_SERIALIZER =
            RECIPE_SERIALIZERS.register("smoke_dye", () -> new SimpleCraftingRecipeSerializer<>(SmokeDyeRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VehicleAssemblingRecipe>> VEHICLE_ASSEMBLING_SERIALIZER =
            RECIPE_SERIALIZERS.register("vehicle_assembling", VehicleAssemblingRecipeSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VehicleResetRecipe>> VEHICLE_RESET_SERIALIZER =
            RECIPE_SERIALIZERS.register("vehicle_reset", () -> new SimpleCraftingRecipeSerializer<>(VehicleResetRecipe::new));

    public static final DeferredHolder<RecipeType<?>, RecipeType<VehicleAssemblingRecipe>> VEHICLE_ASSEMBLING_TYPE =
            RECIPE_TYPES.register("vehicle_assembling", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return Mod.MODID + ":vehicle_assembling";
                }
            });

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
    }
}
