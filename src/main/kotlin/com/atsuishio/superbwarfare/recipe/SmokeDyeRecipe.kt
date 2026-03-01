package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.item.projectile.M18SmokeGrenadeItem
import net.minecraft.core.HolderLookup
import net.minecraft.util.FastColor
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level
import javax.annotation.ParametersAreNonnullByDefault
import kotlin.math.max

class SmokeDyeRecipe(pCategory: CraftingBookCategory) : CustomRecipe(pCategory) {
    override fun matches(input: CraftingInput, pLevel: Level): Boolean {
        var itemStack = ItemStack.EMPTY
        val list: MutableList<ItemStack> = mutableListOf()

        for (stack in input.items()) {
            if (stack.isEmpty) continue
            if (stack.`is`(ModItems.M18_SMOKE_GRENADE.get())) {
                if (!itemStack.isEmpty) {
                    return false
                }

                itemStack = stack
            } else {
                if (stack.item !is DyeItem) {
                    return false
                }

                list.add(stack)
            }
        }
        return !itemStack.isEmpty && !list.isEmpty()
    }

    @ParametersAreNonnullByDefault
    override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack {
        val list = mutableListOf<DyeItem>()
        var itemStack = ItemStack.EMPTY

        for (stack in input.items()) {
            if (!stack.isEmpty) {
                val item = stack.item
                if (stack.`is`(ModItems.M18_SMOKE_GRENADE.get())) {
                    if (!itemStack.isEmpty) {
                        return ItemStack.EMPTY
                    }
                    itemStack = stack.copy()
                } else {
                    if (item !is DyeItem) {
                        return ItemStack.EMPTY
                    }
                    list.add(item)
                }
            }
        }

        return if (!itemStack.isEmpty && !list.isEmpty()) dyeItem(itemStack, list) else ItemStack.EMPTY
    }

    override fun canCraftInDimensions(pWidth: Int, pHeight: Int): Boolean {
        return pWidth * pHeight >= 2
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return ModRecipes.SMOKE_DYE_SERIALIZER.get()
    }

    companion object {
        fun dyeItem(pStack: ItemStack, pDyes: MutableList<DyeItem>): ItemStack {
            val stack: ItemStack
            val colors = IntArray(3)
            var i = 0
            var j = 0
            val item = pStack.item
            if (item is M18SmokeGrenadeItem) {
                stack = pStack.copyWithCount(1)
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

                for (dyeItem in pDyes) {
                    val dyeColors = dyeItem.dyeColor.textureDiffuseColor

                    val r = FastColor.ARGB32.red(dyeColors)
                    val g = FastColor.ARGB32.green(dyeColors)
                    val b = FastColor.ARGB32.blue(dyeColors)

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
            item.setColor(stack, color)
            return stack
        }
    }
}
