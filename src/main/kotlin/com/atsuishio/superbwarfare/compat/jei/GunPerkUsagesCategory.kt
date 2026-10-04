package com.atsuishio.superbwarfare.compat.jei

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.item.misc.PerkItem
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
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class GunPerkUsagesCategory(helper: IGuiHelper) : IRecipeCategory<PerkUsageRecipe> {
    private val icon: IDrawable =
        helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, ItemStack(ModItems.REFORGING_TABLE.get()))

    override fun getRecipeType(): RecipeType<PerkUsageRecipe> = TYPE

    override fun getTitle(): Component = Component.translatable("jei.superbwarfare.gun_perk_usages")

    override fun getIcon(): IDrawable = this.icon

    override fun getWidth(): Int = WIDTH

    override fun getHeight(): Int = HEIGHT

    override fun isHandled(recipe: PerkUsageRecipe): Boolean = recipe.guns.isNotEmpty()

    override fun draw(
        recipe: PerkUsageRecipe,
        recipeSlotsView: IRecipeSlotsView,
        guiGraphics: GuiGraphics,
        mouseX: Double,
        mouseY: Double
    ) {
        val name = recipe.perk.hoverName
        guiGraphics.drawString(
            mc.font, name,
            WIDTH / 2 - mc.font.width(name) / 2, 5, 5592405, false
        )
    }

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: PerkUsageRecipe, focuses: IFocusGroup) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
            .addItemStack(recipe.perk)
            .setStandardSlotBackground()

        for (gun in recipe.guns) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY).addItemStack(gun)
        }
    }

    override fun createRecipeExtras(
        builder: IRecipeExtrasBuilder,
        recipe: PerkUsageRecipe,
        focuses: IFocusGroup
    ) {
        val slots = builder.recipeSlots.getSlots(RecipeIngredientRole.RENDER_ONLY)
        if (slots.isEmpty()) return
        builder.addScrollGridWidget(slots, COLUMNS, VISIBLE_ROWS).setPosition(1, 21)
    }

    companion object {
        val TYPE: RecipeType<PerkUsageRecipe> =
            RecipeType.create(Mod.MODID, "gun_perk_usages", PerkUsageRecipe::class.java)

        private const val WIDTH = 144
        private const val HEIGHT = 112
        private const val COLUMNS = 7
        private const val VISIBLE_ROWS = (HEIGHT - 21) / 18

        @JvmStatic
        fun createRecipes(guns: List<ItemStack>): List<PerkUsageRecipe> {
            val byName = LinkedHashMap<String, Item>()
            val byPerk = LinkedHashMap<Perk, Item>()

            for (entry in ModItems.PERKS.getEntries()) {
                val item = entry.get()
                if (item !is PerkItem) continue

                byName[item.perk.descriptionId] = item
                byPerk[item.perk] = item
            }

            val accepting = LinkedHashMap<Item, LinkedHashSet<ItemStack>>()

            for (gun in guns) {
                if (gun.item !is GunItem) continue

                for (perk in from(gun).availablePerks()) {
                    val item = byPerk[perk] ?: byName[perk.descriptionId] ?: continue
                    accepting.getOrPut(item) { LinkedHashSet() }.add(gun.copy())
                }
            }

            return accepting.map { (item, owners) -> PerkUsageRecipe(ItemStack(item), owners.toList()) }
        }
    }
}

data class PerkUsageRecipe(
    val perk: ItemStack,
    val guns: List<ItemStack>
)
