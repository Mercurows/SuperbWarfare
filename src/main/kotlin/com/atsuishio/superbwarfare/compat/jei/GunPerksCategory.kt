package com.atsuishio.superbwarfare.compat.jei

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.mc
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
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

class GunPerksCategory(helper: IGuiHelper) : IRecipeCategory<ItemStack> {
    private val background: IDrawable = helper.drawableBuilder(TEXTURE, 0, 0, 144, 128)
        .setTextureSize(144, 128)
        .build()
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
            80 - mc.font.width(name) / 2, 5, 5592405, false
        )
    }

    @Deprecated("Deprecated in Java")
    @Suppress("removal")
    override fun getBackground(): IDrawable {
        return this.background
    }

    override fun getRecipeType(): RecipeType<ItemStack> {
        return TYPE
    }

    override fun getTitle(): Component {
        return Component.translatable("jei.superbwarfare.gun_perks")
    }

    override fun getIcon(): IDrawable {
        return this.icon
    }

    override fun getWidth(): Int {
        return 144
    }

    override fun getHeight(): Int {
        return 128
    }

    override fun setRecipe(builder: IRecipeLayoutBuilder, stack: ItemStack, focuses: IFocusGroup) {
        if (stack.item !is GunItem) return
        val data = from(stack)
        val perks = data.availablePerks()
        val sortedPerks = perks.toMutableList()
        sortedPerks.sortWith { a, b ->
            val aIndex = getIndex(a)
            val bIndex = getIndex(b)
            if (aIndex == bIndex) a.name.compareTo(b.name) else aIndex - bIndex
        }

        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).addItemStack(stack)

        for (i in sortedPerks.indices) {
            val perkItem = sortedPerks[i].getItem().get()
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 8) * 18, 21 + i / 8 * 18)
                .addItemStack(perkItem.defaultInstance)
        }
    }

    companion object {
        val TEXTURE: ResourceLocation = loc("textures/gui/jei_gun_perks.png")
        val TYPE: RecipeType<ItemStack> = RecipeType.create(Mod.MODID, "gun_perks", ItemStack::class.java)

        private fun getIndex(perk: Perk): Int {
            return when (perk.type) {
                Perk.Type.AMMO -> 0
                Perk.Type.FUNCTIONAL -> 1
                Perk.Type.DAMAGE -> 2
            }
        }
    }
}
