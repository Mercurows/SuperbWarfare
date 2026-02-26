package com.atsuishio.superbwarfare.compat.jei

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

class VehicleAssemblingCategory(helper: IGuiHelper) : IRecipeCategory<VehicleAssemblingRecipe> {
    private val background: IDrawable = helper.drawableBuilder(TEXTURE, 0, 0, 144, 36)
        .setTextureSize(144, 36)
        .build()
    private val icon: IDrawable = helper.createDrawableIngredient(
        VanillaTypes.ITEM_STACK,
        ItemStack(ModItems.VEHICLE_ASSEMBLING_TABLE.get())
    )

    @Deprecated("Deprecated in Java")
    @Suppress("removal")
    override fun getBackground(): IDrawable {
        return this.background
    }

    override fun getRecipeType(): RecipeType<VehicleAssemblingRecipe> {
        return TYPE
    }

    override fun getTitle(): Component {
        return Component.translatable("jei.superbwarfare.vehicle_assembling")
    }

    override fun getIcon(): IDrawable {
        return this.icon
    }

    override fun getWidth(): Int {
        return 144
    }

    override fun getHeight(): Int {
        return 36
    }

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: VehicleAssemblingRecipe, focuses: IFocusGroup) {
        val res = recipe.result
        builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 1).addItemStack(res.getResult().copyWithCount(res.count))

        for (i in recipe.inputs.indices) {
            if (i >= 12) return
            val ingredient = recipe.inputs[i].ingredient.getItems()
            ingredient.forEach { it.count = recipe.inputs[i].count }
            builder.addSlot(RecipeIngredientRole.INPUT, 37 + (i % 6) * 18, 1 + i / 6 * 18)
                .addItemStacks(listOf(*ingredient))
        }
    }

    companion object {
        val TEXTURE: ResourceLocation = loc("textures/gui/jei_vehicle_assembling_table.png")
        val TYPE: RecipeType<VehicleAssemblingRecipe> =
            RecipeType<VehicleAssemblingRecipe>(loc("vehicle_assembling"), VehicleAssemblingRecipe::class.java)
    }
}
