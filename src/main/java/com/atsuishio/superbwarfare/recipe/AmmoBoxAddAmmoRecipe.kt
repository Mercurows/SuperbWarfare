package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.item.common.ammo.AmmoBoxItem
import com.atsuishio.superbwarfare.item.common.ammo.AmmoSupplierItem
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level
import javax.annotation.ParametersAreNonnullByDefault

class AmmoBoxAddAmmoRecipe(id: ResourceLocation, category: CraftingBookCategory) : CustomRecipe(id, category) {
    override fun matches(input: CraftingContainer, pLevel: Level): Boolean {

        var type: Ammo? = null
        var ammoBox: ItemStack? = null
        var ammoCount = 0

        for (stack in input.items) {
            val item = stack.item
            if (item is AmmoBoxItem) {
                if (ammoBox != null) return false
                ammoBox = stack
            } else if (item is AmmoSupplierItem) {
                if (type == null) {
                    type = item.type
                } else if (type != item.type) {
                    return false
                }

                ammoCount += type.get(stack)
            } else if (!stack.isEmpty) {
                return false
            }
        }

        if (type == null || ammoBox == null) return false
        if (ammoCount + type.get(ammoBox) > type.limit) return false

        return true
    }


    private fun addAmmo(map: MutableMap<Ammo, Int>, type: Ammo, count: Int) {
        map[type] = map.getOrDefault(type, 0) + count
    }

    @ParametersAreNonnullByDefault
    override fun assemble(input: CraftingContainer, registryAccess: RegistryAccess): ItemStack {
        val map = mutableMapOf<Ammo, Int>()
        var ammoBox = ItemStack.EMPTY

        for (stack in input.items) {
            val item = stack.item
            if (item is AmmoSupplierItem) {
                addAmmo(map, item.type, item.ammoToAdd)
            } else if (item is AmmoBoxItem) {
                ammoBox = stack.copy()
                for (type in Ammo.entries) {
                    addAmmo(map, type, type.get(stack))
                }
            }
        }

        for (type in Ammo.entries) {
            type.set(ammoBox, map.getOrDefault(type, 0))
        }

        return ammoBox
    }

    override fun canCraftInDimensions(pWidth: Int, pHeight: Int) = true

    override fun getSerializer(): RecipeSerializer<AmmoBoxAddAmmoRecipe> = ModRecipes.AMMO_BOX_ADD_AMMO_SERIALIZER.get()
}
