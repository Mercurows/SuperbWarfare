package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.item.IDyeableSmokeItem
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level
import kotlin.math.max

class SmokeDyeRecipe(pId: ResourceLocation, pCategory: CraftingBookCategory) : CustomRecipe(pId, pCategory) {
    override fun matches(pInv: CraftingContainer, pLevel: Level): Boolean {
        var itemstack = ItemStack.EMPTY
        val list: MutableList<ItemStack> = mutableListOf()

        for (i in 0..<pInv.containerSize) {
            val stack = pInv.getItem(i)
            if (!stack.isEmpty) {
                if (stack.item is IDyeableSmokeItem) {
                    if (!itemstack.isEmpty) {
                        return false
                    }

                    itemstack = stack
                } else {
                    if (stack.item !is DyeItem) {
                        return false
                    }

                    list.add(stack)
                }
            }
        }
        return !itemstack.isEmpty && !list.isEmpty()
    }

    override fun assemble(pContainer: CraftingContainer, pRegistryAccess: RegistryAccess): ItemStack {
        val list: MutableList<DyeItem> = mutableListOf()
        var itemstack = ItemStack.EMPTY

        for (i in 0..<pContainer.containerSize) {
            val stack = pContainer.getItem(i)
            if (!stack.isEmpty) {
                val item = stack.item
                if (stack.item is IDyeableSmokeItem) {
                    if (!itemstack.isEmpty) {
                        return ItemStack.EMPTY
                    }
                    itemstack = stack.copy()
                } else {
                    if (item !is DyeItem) {
                        return ItemStack.EMPTY
                    }
                    list.add(item)
                }
            }
        }

        return if (!itemstack.isEmpty && !list.isEmpty()) dyeItem(itemstack, list) else ItemStack.EMPTY
    }

    override fun canCraftInDimensions(pWidth: Int, pHeight: Int): Boolean {
        return pWidth * pHeight >= 2
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return ModRecipes.SMOKE_DYE_SERIALIZER.get()
    }

    companion object {
        fun dyeItem(pStack: ItemStack, pDyes: MutableList<DyeItem>): ItemStack {
            val itemstack: ItemStack
            val colors = IntArray(3)
            var i = 0
            var j = 0
            val item = pStack.item
            if (item is IDyeableSmokeItem) {
                itemstack = pStack.copyWithCount(1)
                val color: Int = item.getColor(pStack)
                if (color != 0xFFFFFF) {
                    val r = (color shr 16 and 255).toFloat() / 255f
                    val g = (color shr 8 and 255).toFloat() / 255f
                    val b = (color and 255).toFloat() / 255f
                    i += (max(r, max(g, b)) * 255f).toInt()
                    colors[0] += (r * 255f).toInt()
                    colors[1] += (g * 255f).toInt()
                    colors[2] += (b * 255f).toInt()
                    ++j
                }

                for (dye in pDyes) {
                    val dyeColors = dye.dyeColor.textureDiffuseColors
                    val r = (dyeColors[0] * 255f).toInt()
                    val g = (dyeColors[1] * 255f).toInt()
                    val b = (dyeColors[2] * 255f).toInt()
                    i += max(r, max(g, b))
                    colors[0] += r
                    colors[1] += g
                    colors[2] += b
                    ++j
                }
            } else {
                return ItemStack.EMPTY
            }

            var red = colors[0] / j
            var green = colors[1] / j
            var blue = colors[2] / j
            val rate = i.toFloat() / j.toFloat()
            val max = max(red, max(green, blue)).toFloat()
            red = (red.toFloat() * rate / max).toInt()
            green = (green.toFloat() * rate / max).toInt()
            blue = (blue.toFloat() * rate / max).toInt()
            var color = (red shl 8) + green
            color = (color shl 8) + blue
            item.setColor(itemstack, color)
            return itemstack
        }
    }
}
