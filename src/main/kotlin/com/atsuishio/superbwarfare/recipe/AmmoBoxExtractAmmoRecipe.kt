package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.item.ammo.AmmoBoxItem
import com.atsuishio.superbwarfare.item.ammo.ammoBoxData
import net.minecraft.core.NonNullList
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level
import javax.annotation.ParametersAreNonnullByDefault

class AmmoBoxExtractAmmoRecipe(id: ResourceLocation, category: CraftingBookCategory) : CustomRecipe(id, category) {
    @ParametersAreNonnullByDefault
    override fun matches(input: CraftingContainer, level: Level): Boolean {
        var hasAmmoBox = false
        var ammoBoxItem = ItemStack.EMPTY

        for (stack in input.items) {
            if (stack.item is AmmoBoxItem) {
                if (hasAmmoBox) return false
                hasAmmoBox = true
                ammoBoxItem = stack
            } else if (!stack.isEmpty) {
                return false
            }
        }

        val type = ammoBoxItem.ammoBoxData.type ?: return false

        return type.get(ammoBoxItem) > 0
    }

    @ParametersAreNonnullByDefault
    override fun assemble(input: CraftingContainer, registryAccess: RegistryAccess): ItemStack {
        var type: Ammo? = null

        for (item in input.items) {
            if (item.item is AmmoBoxItem) {
                type = item.ammoBoxData.type
                break
            }
        }

        requireNotNull(type)

        return type.itemStack
    }

    override fun getRemainingItems(input: CraftingContainer): NonNullList<ItemStack?> {
        val remaining = super.getRemainingItems(input)

        for (i in input.items.indices) {
            val item = input.getItem(i)
            if (item.item is AmmoBoxItem) {
                val ammoBox = item.copy()

                val type = ammoBox.ammoBoxData.type!!

                type.add(ammoBox, -1)
                remaining[i] = ammoBox

                break
            }
        }

        return remaining
    }

    override fun canCraftInDimensions(pWidth: Int, pHeight: Int) = true

    override fun getSerializer(): RecipeSerializer<*> = ModRecipes.AMMO_BOX_EXTRACT_AMMO_SERIALIZER.get()
}
