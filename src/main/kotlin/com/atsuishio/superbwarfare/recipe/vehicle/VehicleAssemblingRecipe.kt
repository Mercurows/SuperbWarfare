package com.atsuishio.superbwarfare.recipe.vehicle

import com.atsuishio.superbwarfare.init.ModRecipes
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level

class VehicleAssemblingRecipe(
    private val id: ResourceLocation,
    @JvmField val category: Category,
    @JvmField val result: VehicleAssemblingResult,
    @JvmField val inputs: MutableList<VehicleAssemblingIngredient>
) : Recipe<Inventory> {
    constructor(id: ResourceLocation, data: VehicleAssemblingRecipeData) : this(
        id,
        Category.getCategory(data.category),
        data.result!!,
        data.getInputs()!!
    )

    override fun matches(pContainer: Inventory, pLevel: Level): Boolean {
        return false
    }

    override fun assemble(pContainer: Inventory, pRegistryAccess: RegistryAccess): ItemStack {
        return ItemStack.EMPTY
    }

    override fun canCraftInDimensions(pWidth: Int, pHeight: Int): Boolean {
        return true
    }

    override fun getResultItem(pRegistryAccess: RegistryAccess): ItemStack {
        return this.result.getResult().copy()
    }

    override fun getId(): ResourceLocation {
        return this.id
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
            fun getCategory(name: String): Category {
                for (category in Category.entries) {
                    if (category.typeName == name) {
                        return category
                    }
                }
                return MISC
            }
        }
    }
}
