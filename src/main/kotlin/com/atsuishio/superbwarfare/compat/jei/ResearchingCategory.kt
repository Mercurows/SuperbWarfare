package com.atsuishio.superbwarfare.compat.jei

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.recipe.ResearchingRecipe
import com.atsuishio.superbwarfare.tools.mc
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.drawable.IDrawableAnimated
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient

class ResearchingCategory(helper: IGuiHelper) : IRecipeCategory<ResearchingRecipe> {
    private val background: IDrawable = helper.drawableBuilder(TEXTURE, 0, 0, 138, 58)
        .setTextureSize(256, 128)
        .build()
    private val icon: IDrawable = helper.createDrawableIngredient(
        VanillaTypes.ITEM_STACK,
        ItemStack(ModItems.BLUEPRINT_RESEARCH_TABLE.get())
    )
    private val progress: IDrawableAnimated = helper.drawableBuilder(TEXTURE, 0, 59, 128, 32)
        .setTextureSize(256, 128)
        .buildAnimated(100, IDrawableAnimated.StartDirection.LEFT, false)

    @Deprecated("Deprecated in Java")
    @Suppress("removal")
    override fun getBackground(): IDrawable = this.background

    override fun getWidth(): Int = 138

    override fun getHeight(): Int = 58

    override fun getRecipeType(): RecipeType<ResearchingRecipe> = TYPE

    override fun getTitle(): Component = Component.translatable("jei.superbwarfare.researching")

    override fun getIcon(): IDrawable = this.icon

    override fun draw(
        recipe: ResearchingRecipe,
        recipeSlotsView: IRecipeSlotsView,
        guiGraphics: GuiGraphics,
        mouseX: Double,
        mouseY: Double
    ) {
        this.progress.draw(guiGraphics, 5, 24)

        guiGraphics.drawString(
            mc.font,
            Component.translatable(
                "gui.jei.category.smelting.time.seconds",
                (recipe.time / 20f).toString().format("##.#")
            ),
            36, 3, 0xdedede, false
        )
        if (recipe.selectable) {
            guiGraphics.drawString(
                mc.font,
                Component.translatable("jei.superbwarfare.researching.selectable"),
                36, 13, 0xff8426, false
            )
        }

        val color = recipe.color
        val colorU = when (color) {
            1, 3 -> 139
            else -> 176
        }
        val colorV = when (color) {
            1, 2 -> 0
            else -> 25
        }
        if (color != 0) {
            guiGraphics.blit(TEXTURE, 50, 28, colorU.toFloat(), colorV.toFloat(),
                36, 24, 256, 128)
        }
    }

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: ResearchingRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 11, 32).addIngredients(recipe.input)
        builder.addSlot(RecipeIngredientRole.INPUT, 99, 3).addIngredients(recipe.base)
        builder.addSlot(RecipeIngredientRole.INPUT, 119, 3).addIngredients(recipe.addition)
        builder.addSlot(RecipeIngredientRole.CATALYST, 60, 32).addIngredients(recipe.special)

        val result = recipe.result
        if (!result.isRandom()) {
            val resItem = result.getResult()
            builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 32).addItemStack(resItem)
        } else {
            val resItems = result.getResultList()
            val count = result.count
            val list = resItems.map { ItemStack(it, count) }.toMutableList()
            builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 32).addItemStacks(list)
        }

        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 11, 3)
            .addIngredients(Ingredient.of(ModTags.Items.RESEARCH_FUEL))
    }

    companion object {
        val TEXTURE: ResourceLocation = loc("textures/gui/jei_blueprint_research_table.png")
        val TYPE: RecipeType<ResearchingRecipe> =
            RecipeType<ResearchingRecipe>(loc("researching"), ResearchingRecipe::class.java)
    }
}