package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.recipe.PotionMortarShellRecipe
import com.atsuishio.superbwarfare.recipe.ResearchingRecipe
import com.atsuishio.superbwarfare.recipe.SmokeDyeRecipe
import com.atsuishio.superbwarfare.recipe.VehicleResetRecipe
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipeSerializer
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModRecipes {
    @JvmField
    val RECIPE_SERIALIZERS: DeferredRegister<RecipeSerializer<*>> =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Mod.MODID)

    @JvmField
    val RECIPE_TYPES: DeferredRegister<RecipeType<*>> =
        DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Mod.MODID)

    @JvmField
    val POTION_MORTAR_SHELL_SERIALIZER: RegistryObject<RecipeSerializer<PotionMortarShellRecipe>> =
        RECIPE_SERIALIZERS.register("potion_mortar_shell") {
            SimpleCraftingRecipeSerializer { id, category ->
                PotionMortarShellRecipe(id, category)
            }
        }

    @JvmField
    val SMOKE_DYE_SERIALIZER: RegistryObject<RecipeSerializer<SmokeDyeRecipe>> =
        RECIPE_SERIALIZERS.register("smoke_dye") {
            SimpleCraftingRecipeSerializer { id, category ->
                SmokeDyeRecipe(id, category)
            }
        }

    @JvmField
    val VEHICLE_ASSEMBLING_SERIALIZER: RegistryObject<RecipeSerializer<VehicleAssemblingRecipe>> =
        RECIPE_SERIALIZERS.register("vehicle_assembling") { VehicleAssemblingRecipeSerializer() }

    @JvmField
    val VEHICLE_RESET_SERIALIZER: RegistryObject<RecipeSerializer<VehicleResetRecipe>> =
        RECIPE_SERIALIZERS.register("vehicle_reset") {
            SimpleCraftingRecipeSerializer { id, category ->
                VehicleResetRecipe(id, category)
            }
        }

    @JvmField
    val RESEARCHING_SERIALIZER: RegistryObject<RecipeSerializer<ResearchingRecipe>> =
        RECIPE_SERIALIZERS.register("researching") { ResearchingRecipe.Serializer() }

    @JvmField
    val VEHICLE_ASSEMBLING_TYPE: RegistryObject<RecipeType<VehicleAssemblingRecipe>> =
        RECIPE_TYPES.register("vehicle_assembling") {
            object : RecipeType<VehicleAssemblingRecipe> {
                override fun toString(): String {
                    return Mod.MODID + ":vehicle_assembling"
                }
            }
        }

    @JvmField
    val RESEARCHING_TYPE: RegistryObject<RecipeType<ResearchingRecipe>> =
        RECIPE_TYPES.register("researching") {
            object : RecipeType<ResearchingRecipe> {
                override fun toString(): String {
                    return Mod.MODID + ":researching"
                }
            }
        }

    fun register(bus: IEventBus) {
        RECIPE_SERIALIZERS.register(bus)
        RECIPE_TYPES.register(bus)
    }
}
