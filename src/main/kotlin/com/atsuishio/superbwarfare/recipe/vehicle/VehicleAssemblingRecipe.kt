package com.atsuishio.superbwarfare.recipe.vehicle

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModRecipes
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.neoforged.neoforge.items.wrapper.RecipeWrapper

class VehicleAssemblingRecipe : Recipe<RecipeWrapper> {
    @JvmField
    val category: Category
    @JvmField
    val result: VehicleAssemblingResult
    @JvmField
    val inputs: MutableList<VehicleAssemblingIngredient>

    constructor(
        inputs: MutableList<VehicleAssemblingIngredient>,
        recipeCategory: String,
        result: VehicleAssemblingResult
    ) {
        this.category = Category.valueOf(recipeCategory)
        this.result = result
        this.inputs = inputs
    }

    constructor(
        inputs: MutableList<VehicleAssemblingIngredient>,
        recipeCategory: Category,
        result: VehicleAssemblingResult
    ) {
        this.category = recipeCategory
        this.result = result
        this.inputs = inputs
    }

    override fun matches(pContainer: RecipeWrapper, pLevel: Level): Boolean {
        return false
    }

    override fun assemble(recipeWrapper: RecipeWrapper, provider: HolderLookup.Provider): ItemStack {
        return ItemStack.EMPTY
    }

    override fun canCraftInDimensions(pWidth: Int, pHeight: Int): Boolean {
        return true
    }

    override fun getResultItem(provider: HolderLookup.Provider): ItemStack {
        return this.result.getResult().copy()
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return ModRecipes.VEHICLE_ASSEMBLING_SERIALIZER.get()
    }

    override fun getType(): RecipeType<*> {
        return ModRecipes.VEHICLE_ASSEMBLING_TYPE.get()
    }

    enum class Category(@JvmField val typeName: String) {
        LAND("land"),
        DEFENSE("defense"),
        AIRCRAFT("aircraft"),
        CIVILIAN("civilian"),
        WATER("water"),
        MISC("misc");

        companion object {
            fun getCategory(name: String?): Category {
                for (category in entries) {
                    if (category.typeName == name) {
                        return category
                    }
                }
                return MISC
            }
        }
    }

    companion object {
        fun create(
            ingredients: MutableMap<String, Int>,
            recipeCategory: Category,
            type: EntityType<*>
        ): VehicleAssemblingRecipe {
            val inputs = arrayListOf<VehicleAssemblingIngredient>()
            for (entry in ingredients.entries) {
                inputs.add(VehicleAssemblingIngredient(entry.key, entry.value))
            }
            val result = VehicleAssemblingResult(
                ModItems.CONTAINER.id.toString(),
                BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                1
            )
            return VehicleAssemblingRecipe(inputs, recipeCategory, result)
        }

        fun create(
            ingredients: MutableMap<String, Int>,
            recipeCategory: Category,
            result: Item,
            count: Int
        ): VehicleAssemblingRecipe {
            val inputs = arrayListOf<VehicleAssemblingIngredient>()
            for (entry in ingredients.entries) {
                inputs.add(VehicleAssemblingIngredient(entry.key, entry.value))
            }
            return VehicleAssemblingRecipe(
                inputs,
                recipeCategory,
                VehicleAssemblingResult(BuiltInRegistries.ITEM.getKey(result).toString(), "", count)
            )
        }
    }
}
