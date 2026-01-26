package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModRecipes
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

class PotionMortarShellRecipe(pId: ResourceLocation, pCategory: CraftingBookCategory) : CustomRecipe(pId, pCategory) {
    override fun matches(pContainer: CraftingContainer, pLevel: Level): Boolean {
        if (pContainer.width == 3 && pContainer.height == 3) {
            for (i in 0..<pContainer.width) {
                for (j in 0..<pContainer.height) {
                    val index = i + j * pContainer.width

                    val itemstack = pContainer.getItem(index)

                    if (index % 2 == 0) {
                        if (i == 1 && j == 1) {
                            if (!itemstack.`is`(Items.LINGERING_POTION)) {
                                return false
                            }
                        } else if (!itemstack.isEmpty) {
                            return false
                        }
                    } else if (!itemstack.`is`(ModItems.MORTAR_SHELL.get())) {
                        return false
                    }
                }
            }
            return true
        } else {
            return false
        }
    }

    override fun assemble(pContainer: CraftingContainer, pRegistryAccess: RegistryAccess): ItemStack {
        val itemstack = pContainer.getItem(1 + pContainer.width)
        if (!itemstack.`is`(Items.LINGERING_POTION)) {
            return ItemStack.EMPTY
        } else {
            val res = ItemStack(ModItems.POTION_MORTAR_SHELL.get(), 4)
            PotionUtils.setPotion(res, PotionUtils.getPotion(itemstack))
            PotionUtils.setCustomEffects(res, PotionUtils.getCustomEffects(itemstack))
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
