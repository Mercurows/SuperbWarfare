package com.atsuishio.superbwarfare.compat.jei

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.gun.GunItem
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
import net.minecraftforge.registries.ForgeRegistries

class GunAttachmentsCategory(helper: IGuiHelper) : IRecipeCategory<AttachmentRecipe> {
    private val icon: IDrawable =
        helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, ItemStack(ModItems.MAGAZINE_EXTEND.get()))

    override fun getRecipeType(): RecipeType<AttachmentRecipe> = TYPE

    override fun getTitle(): Component = Component.translatable("jei.superbwarfare.gun_attachments")

    override fun getIcon(): IDrawable = this.icon

    override fun getWidth(): Int = WIDTH

    override fun getHeight(): Int = HEIGHT

    override fun isHandled(recipe: AttachmentRecipe): Boolean = recipe.itemsBySlot.isNotEmpty() && !recipe.gun.isEmpty

    override fun draw(
        recipe: AttachmentRecipe,
        recipeSlotsView: IRecipeSlotsView,
        guiGraphics: GuiGraphics,
        mouseX: Double,
        mouseY: Double
    ) {
        val name = recipe.gun.hoverName
        guiGraphics.drawString(
            mc.font, name,
            WIDTH / 2 - mc.font.width(name) / 2, 5, 5592405, false
        )
    }

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: AttachmentRecipe, focuses: IFocusGroup) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
            .addItemStack(recipe.gun)
            .setStandardSlotBackground()

        for ((_, stacks) in recipe.itemsBySlot) {
            for (stack in stacks) {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY).addItemStack(stack)
            }
        }
    }

    override fun createRecipeExtras(
        builder: IRecipeExtrasBuilder,
        recipe: AttachmentRecipe,
        focuses: IFocusGroup
    ) {
        val slots = builder.recipeSlots.getSlots(RecipeIngredientRole.RENDER_ONLY)
        if (slots.isEmpty()) return
        builder.addScrollGridWidget(slots, COLUMNS, VISIBLE_ROWS).setPosition(1, 21)
    }

    companion object {
        val TYPE: RecipeType<AttachmentRecipe> =
            RecipeType.create(Mod.MODID, "gun_attachments", AttachmentRecipe::class.java)

        private const val WIDTH = 144
        private const val HEIGHT = 112
        private const val COLUMNS = 7
        private const val VISIBLE_ROWS = (HEIGHT - 21) / 18

        @JvmStatic
        fun createRecipes(guns: List<ItemStack>): List<AttachmentRecipe> {
            val bySlot = ModItems.ATTACHMENTS.entries
                .map { ItemStack(it.get()) }
                .mapNotNull { stack ->
                    val id = ForgeRegistries.ITEMS.getKey(stack.item) ?: return@mapNotNull null
                    AttachmentDefinition.from(id)?.let { it.slot to stack }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, stacks) -> stacks.sortedBy { ForgeRegistries.ITEMS.getKey(it.item)?.toString() } }

            return guns.mapNotNull { gun ->
                if (gun.item !is GunItem) return@mapNotNull null

                val data = from(gun)
                val items = AttachmentType.entries.mapNotNull { slot ->
                    val available = data.availableAttachments(slot).toSet()
                    bySlot[slot].orEmpty()
                        .filter { ForgeRegistries.ITEMS.getKey(it.item) in available }
                        .takeIf { it.isNotEmpty() }
                        ?.let { slot to it }
                }

                if (items.isEmpty()) null else AttachmentRecipe(gun.copy(), items)
            }
        }
    }
}

data class AttachmentRecipe(
    val gun: ItemStack,
    val itemsBySlot: List<Pair<AttachmentType, List<ItemStack>>>
)
