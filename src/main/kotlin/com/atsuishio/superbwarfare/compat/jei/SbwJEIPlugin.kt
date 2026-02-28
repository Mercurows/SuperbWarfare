package com.atsuishio.superbwarfare.compat.jei

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.screens.BlueprintResearchTableScreen
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.clientLevel
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.constants.RecipeTypes
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.registration.*
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraftforge.registries.ForgeRegistries
import java.util.*

@JeiPlugin
class SbwJEIPlugin : IModPlugin {
    override fun getPluginUid(): ResourceLocation {
        return loc("jei_plugin")
    }

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        Companion.jeiRuntime = jeiRuntime
    }

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        registration.addRecipeCategories(GunPerksCategory(registration.jeiHelpers.guiHelper))
        registration.addRecipeCategories(VehicleAssemblingCategory(registration.jeiHelpers.guiHelper))
        registration.addRecipeCategories(ResearchingCategory(registration.jeiHelpers.guiHelper))
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        registration.addRecipeCatalyst(ItemStack(ModItems.REFORGING_TABLE.get()), GunPerksCategory.TYPE)
        registration.addRecipeCatalyst(
            ItemStack(ModItems.VEHICLE_ASSEMBLING_TABLE.get()),
            VehicleAssemblingCategory.TYPE
        )
        registration.addRecipeCatalyst(
            ItemStack(ModItems.BLUEPRINT_RESEARCH_TABLE.get()),
            ResearchingCategory.TYPE
        )
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        val level = clientLevel ?: return
        val recipeManager = level.recipeManager

        val guns = ForgeRegistries.ITEMS.getValues().stream().filter { it is GunItem }
            .map { obj -> obj.defaultInstance }.toList()
        registration.addRecipes(GunPerksCategory.TYPE, guns)
        registration.addRecipes(
            VehicleAssemblingCategory.TYPE,
            recipeManager.getAllRecipesFor(ModRecipes.VEHICLE_ASSEMBLING_TYPE.get())
        )
        registration.addRecipes(
            ResearchingCategory.TYPE,
            recipeManager.getAllRecipesFor(ModRecipes.RESEARCHING_TYPE.get())
        )

        registration.addItemStackInfo(
            ItemStack(ModItems.ANCIENT_CPU.get()),
            Component.translatable("jei.superbwarfare.ancient_cpu")
        )
        registration.addItemStackInfo(
            ItemStack(ModItems.CHARGING_STATION.get()),
            Component.translatable("jei.superbwarfare.charging_station")
        )

        val specialCraftingRecipes = PotionMortarShellRecipeMaker.createRecipes()
        registration.addRecipes(RecipeTypes.CRAFTING, specialCraftingRecipes)
    }

    override fun registerItemSubtypes(registration: ISubtypeRegistration) {
        registration.registerSubtypeInterpreter(ModItems.CONTAINER.get()) { ingredient, _ ->
            if (ingredient.tag == null) return@registerSubtypeInterpreter IIngredientSubtypeInterpreter.NONE
            ingredient.tag!!.getCompound("BlockEntityTag").getString("EntityType")
        }

        registration.registerSubtypeInterpreter(ModItems.POTION_MORTAR_SHELL.get()) { stack, _ ->
            if (!stack.hasTag()) {
                return@registerSubtypeInterpreter IIngredientSubtypeInterpreter.NONE
            }
            val potionType = PotionUtils.getPotion(stack)
            val potionTypeString = potionType.getName("")
            val stringBuilder = StringBuilder(potionTypeString)
            val effects = PotionUtils.getMobEffects(stack)
            for (effect in effects) {
                stringBuilder.append(";").append(effect)
            }
            stringBuilder.toString()
        }

        registration.registerSubtypeInterpreter(ModItems.C4_BOMB.get()) { ingredient, _ ->
            if (ingredient.tag == null) return@registerSubtypeInterpreter IIngredientSubtypeInterpreter.NONE
            ingredient.tag!!.getBoolean("Control").toString()
        }
    }

    override fun registerGuiHandlers(registration: IGuiHandlerRegistration) {
        registration.addRecipeClickArea(
            BlueprintResearchTableScreen::class.java,
            64, 23, 48, 12, ResearchingCategory.TYPE
        )
    }

    companion object {
        private var jeiRuntime: IJeiRuntime? = null

        fun getJeiRuntime(): Optional<IJeiRuntime> {
            return Optional.ofNullable<IJeiRuntime>(jeiRuntime)
        }

        /**
         * Code based on @Mafuyu404's [TACZ-addon](https://github.com/Mafuyu404/TACZ-addon)
         */
        @JvmStatic
        fun showRecipes(itemStack: ItemStack): Boolean {
            val result = booleanArrayOf(false)
            getJeiRuntime().ifPresent { jeiRuntime ->
                jeiRuntime.ingredientManager.getIngredientTypeChecked(itemStack)
                    .ifPresent { type ->
                        jeiRuntime.recipesGui.show(
                            jeiRuntime.jeiHelpers.focusFactory
                                .createFocus(RecipeIngredientRole.OUTPUT, type, itemStack)
                        )
                        result[0] = true
                    }
            }
            return result[0]
        }
    }
}
