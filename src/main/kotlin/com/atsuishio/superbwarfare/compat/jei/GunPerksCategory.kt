package com.atsuishio.superbwarfare.compat.jei

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.mc
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

class GunPerksCategory(helper: IGuiHelper) : IRecipeCategory<ItemStack> {
    private val icon: IDrawable =
        helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, ItemStack(ModItems.AP_BULLET!!.get()))

    override fun draw(
        recipe: ItemStack,
        recipeSlotsView: IRecipeSlotsView,
        guiGraphics: GuiGraphics,
        mouseX: Double,
        mouseY: Double
    ) {
        val name = recipe.getHoverName()
        guiGraphics.drawString(
            mc.font, name,
            WIDTH / 2 - mc.font.width(name) / 2, 5, 5592405, false
        )
    }

    override fun getRecipeType(): RecipeType<ItemStack> = TYPE

    override fun getTitle(): Component = Component.translatable("jei.superbwarfare.gun_perks")

    override fun getIcon(): IDrawable = this.icon

    override fun getWidth(): Int = WIDTH

    override fun getHeight(): Int = HEIGHT

    override fun setRecipe(builder: IRecipeLayoutBuilder, stack: ItemStack, focuses: IFocusGroup) {
        if (stack.item !is GunItem) return

        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
            .addItemStack(stack)
            .setStandardSlotBackground()

        val perks = from(stack).availablePerks().sortedWith(
            compareBy({ getIndex(it) }, { it.name })
        )

        for (perk in perks) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY)
                .addItemStack(perk.getItem().get().defaultInstance)
        }
    }

    override fun createRecipeExtras(
        builder: IRecipeExtrasBuilder,
        recipe: ItemStack,
        focuses: IFocusGroup
    ) {
        val slots = builder.recipeSlots.getSlots(RecipeIngredientRole.RENDER_ONLY)
        if (slots.isEmpty()) return
        builder.addScrollGridWidget(slots, COLUMNS, VISIBLE_ROWS).setPosition(1, 21)
    }

    companion object {
        val TYPE: RecipeType<ItemStack> = RecipeType.create(Mod.MODID, "gun_perks", ItemStack::class.java)

        private const val WIDTH = 144
        private const val HEIGHT = 112
        private const val COLUMNS = 7
        private const val VISIBLE_ROWS = (HEIGHT - 21) / 18

        private fun getIndex(perk: Perk): Int {
            return when (perk.type) {
                Perk.Type.AMMO -> 0
                Perk.Type.FUNCTIONAL -> 1
                Perk.Type.DAMAGE -> 2
            }
        }
    }
}
