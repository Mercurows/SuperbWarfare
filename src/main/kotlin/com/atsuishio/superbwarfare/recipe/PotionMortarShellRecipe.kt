package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModRecipes
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level
import javax.annotation.ParametersAreNonnullByDefault

class PotionMortarShellRecipe(pCategory: CraftingBookCategory) : CustomRecipe(pCategory) {
    override fun matches(input: CraftingInput, pLevel: Level): Boolean {
        if (input.width() == 3 && input.height() == 3) {
            for (i in 0..<input.width()) {
                for (j in 0..<input.height()) {
                    val index = i + j * input.width()

                    val stack = input.getItem(index)

                    if (index % 2 == 0) {
                        if (i == 1 && j == 1) {
                            if (!stack.`is`(Items.LINGERING_POTION)) {
                                return false
                            }
                        } else if (!stack.isEmpty) {
                            return false
                        }
                    } else if (!stack.`is`(ModItems.MORTAR_SHELL.get())) {
                        return false
                    }
                }
            }
            return true
        } else {
            return false
        }
    }

    @ParametersAreNonnullByDefault
    override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack {
        val stack = input.getItem(1 + input.width())
        if (!stack.`is`(Items.LINGERING_POTION)) {
            return ItemStack.EMPTY
        } else {
            val res = ItemStack(ModItems.POTION_MORTAR_SHELL.get(), 4)
            res.set(
                DataComponents.POTION_CONTENTS,
                stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
            )

            return res
        }
    }

    override fun canCraftInDimensions(pWidth: Int, pHeight: Int): Boolean {
        return pWidth >= 2 && pHeight >= 2
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return ModRecipes.POTION_MORTAR_SHELL_SERIALIZER.get()
    }
}