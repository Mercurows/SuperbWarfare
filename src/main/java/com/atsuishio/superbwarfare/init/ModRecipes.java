package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.recipe.*;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Mod.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Mod.MODID);

    public static final RegistryObject<RecipeSerializer<PotionMortarShellRecipe>> POTION_MORTAR_SHELL_SERIALIZER =
            RECIPE_SERIALIZERS.register("potion_mortar_shell", () -> new SimpleCraftingRecipeSerializer<>(PotionMortarShellRecipe::new));
    public static final RegistryObject<RecipeSerializer<AmmoBoxAddAmmoRecipe>> AMMO_BOX_ADD_AMMO_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammo_box_add_ammo", () -> new SimpleCraftingRecipeSerializer<>(AmmoBoxAddAmmoRecipe::new));
    public static final RegistryObject<RecipeSerializer<AmmoBoxExtractAmmoRecipe>> AMMO_BOX_EXTRACT_AMMO_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammo_box_extract_ammo", () -> new SimpleCraftingRecipeSerializer<>(AmmoBoxExtractAmmoRecipe::new));
    public static final RegistryObject<RecipeSerializer<SmokeDyeRecipe>> SMOKE_DYE_SERIALIZER =
            RECIPE_SERIALIZERS.register("smoke_dye", () -> new SimpleCraftingRecipeSerializer<>(SmokeDyeRecipe::new));
    public static final RegistryObject<RecipeSerializer<VehicleAssemblingRecipe>> VEHICLE_ASSEMBLING_SERIALIZER =
            RECIPE_SERIALIZERS.register("vehicle_assembling", VehicleAssemblingRecipeSerializer::new);
    public static final RegistryObject<RecipeSerializer<VehicleResetRecipe>> VEHICLE_RESET_SERIALIZER =
            RECIPE_SERIALIZERS.register("vehicle_reset", () -> new SimpleCraftingRecipeSerializer<>(VehicleResetRecipe::new));
    public static final RegistryObject<RecipeSerializer<ResearchingRecipe>> RESEARCHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("researching", ResearchingRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<VehicleAssemblingRecipe>> VEHICLE_ASSEMBLING_TYPE =
            RECIPE_TYPES.register("vehicle_assembling", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return Mod.MODID + ":vehicle_assembling";
                }
            });
    public static final RegistryObject<RecipeType<ResearchingRecipe>> RESEARCHING_TYPE =
            RECIPE_TYPES.register("researching", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return Mod.MODID + ":researching";
                }
            });

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
    }
}
