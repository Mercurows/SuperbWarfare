package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.recipe.PotionMortarShellRecipe
import com.atsuishio.superbwarfare.recipe.ResearchingRecipe
import com.atsuishio.superbwarfare.recipe.SmokeDyeRecipe
import com.atsuishio.superbwarfare.recipe.VehicleResetRecipe
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipeSerializer
import com.google.common.base.Supplier
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModRecipes {
    @JvmField
    val RECIPE_SERIALIZERS: DeferredRegister<RecipeSerializer<*>> =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Mod.MODID)

    @JvmField
    val RECIPE_TYPES: DeferredRegister<RecipeType<*>> =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Mod.MODID)

    private fun <T : RecipeSerializer<*>> register(
        name: String,
        serializer: () -> T
    ): DeferredHolder<RecipeSerializer<*>, T> = RECIPE_SERIALIZERS.register(name, Supplier { serializer() })

    @JvmField
    val POTION_MORTAR_SHELL_SERIALIZER =
        register("potion_mortar_shell") {
            SimpleCraftingRecipeSerializer(::PotionMortarShellRecipe)
        }

    @JvmField
    val SMOKE_DYE_SERIALIZER = register("smoke_dye") {
        SimpleCraftingRecipeSerializer(::SmokeDyeRecipe)
    }

    @JvmField
    val VEHICLE_ASSEMBLING_SERIALIZER = register("vehicle_assembling") { VehicleAssemblingRecipeSerializer }

    @JvmField
    val VEHICLE_RESET_SERIALIZER = register("vehicle_reset") {
        SimpleCraftingRecipeSerializer(::VehicleResetRecipe)
    }

    @JvmField
    val RESEARCHING_SERIALIZER = register("researching") { ResearchingRecipe.Serializer }

    @JvmField
    val VEHICLE_ASSEMBLING_TYPE: DeferredHolder<RecipeType<*>, out RecipeType<VehicleAssemblingRecipe>> =
        RECIPE_TYPES.register("vehicle_assembling", Supplier {
            object : RecipeType<VehicleAssemblingRecipe> {
                override fun toString() = Mod.MODID + ":vehicle_assembling"
            }
        })

    @JvmField
    val RESEARCHING_TYPE: DeferredHolder<RecipeType<*>, out RecipeType<ResearchingRecipe>> =
        RECIPE_TYPES.register("researching", Supplier {
            object : RecipeType<ResearchingRecipe> {
                override fun toString() = Mod.MODID + ":researching"
            }
        })

    fun register(bus: IEventBus) {
        RECIPE_SERIALIZERS.register(bus)
        RECIPE_TYPES.register(bus)
    }
}
