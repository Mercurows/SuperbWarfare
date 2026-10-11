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

class GunAttachmentUsagesCategory(helper: IGuiHelper) : IRecipeCategory<AttachmentUsageRecipe> {
    private val icon: IDrawable =
        helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, ItemStack(ModItems.AK_47.get()))

    override fun getRecipeType(): RecipeType<AttachmentUsageRecipe> = TYPE

    override fun getTitle(): Component = Component.translatable("jei.superbwarfare.gun_attachment_usages")

    override fun getIcon(): IDrawable = this.icon

    override fun getWidth(): Int = WIDTH

    override fun getHeight(): Int = HEIGHT

    override fun isHandled(recipe: AttachmentUsageRecipe): Boolean = recipe.guns.isNotEmpty()

    override fun draw(
        recipe: AttachmentUsageRecipe,
        recipeSlotsView: IRecipeSlotsView,
        guiGraphics: GuiGraphics,
        mouseX: Double,
        mouseY: Double
    ) {
        val name = recipe.attachment.hoverName
        guiGraphics.drawString(
            mc.font, name,
            WIDTH / 2 - mc.font.width(name) / 2, 5, 5592405, false
        )
    }

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: AttachmentUsageRecipe, focuses: IFocusGroup) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
            .addItemStack(recipe.attachment)
            .setStandardSlotBackground()

        for (gun in recipe.guns) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY).addItemStack(gun)
        }
    }

    override fun createRecipeExtras(
        builder: IRecipeExtrasBuilder,
        recipe: AttachmentUsageRecipe,
        focuses: IFocusGroup
    ) {
        val slots = builder.recipeSlots.getSlots(RecipeIngredientRole.RENDER_ONLY)
        if (slots.isEmpty()) return
        builder.addScrollGridWidget(slots, COLUMNS, VISIBLE_ROWS).setPosition(1, 21)
    }

    companion object {
        val TYPE: RecipeType<AttachmentUsageRecipe> =
            RecipeType.create(Mod.MODID, "gun_attachment_usages", AttachmentUsageRecipe::class.java)

        private const val WIDTH = 144
        private const val HEIGHT = 112
        private const val COLUMNS = 7
        private const val VISIBLE_ROWS = (HEIGHT - 21) / 18

        @JvmStatic
        fun createRecipes(guns: List<ItemStack>): List<AttachmentUsageRecipe> {
            val bySlot = ModItems.ATTACHMENTS.entries
                .map { ItemStack(it.get()) }
                .mapNotNull { stack ->
                    val id = ForgeRegistries.ITEMS.getKey(stack.item) ?: return@mapNotNull null
                    AttachmentDefinition.from(id)?.let { it.slot to stack }
                }
                .groupBy({ it.first }, { it.second })

            val accepting = LinkedHashMap<ItemStack, LinkedHashSet<ItemStack>>()

            for (gun in guns) {
                if (gun.item !is GunItem) continue

                val data = from(gun)
                for (slot in AttachmentType.entries) {
                    val available = data.availableAttachments(slot).toSet()
                    for (stack in bySlot[slot].orEmpty()) {
                        if (ForgeRegistries.ITEMS.getKey(stack.item) !in available) continue
                        accepting.getOrPut(stack) { LinkedHashSet() }.add(gun.copy())
                    }
                }
            }

            return accepting.map { (attachment, owners) -> AttachmentUsageRecipe(attachment, owners.toList()) }
        }
    }
}

data class AttachmentUsageRecipe(
    val attachment: ItemStack,
    val guns: List<ItemStack>
)
