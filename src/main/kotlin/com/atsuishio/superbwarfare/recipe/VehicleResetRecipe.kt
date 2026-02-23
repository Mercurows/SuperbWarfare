package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

class VehicleResetRecipe(pId: ResourceLocation, pCategory: CraftingBookCategory) : CustomRecipe(pId, pCategory) {
    override fun matches(pContainer: CraftingContainer, pLevel: Level): Boolean {
        var kit = ItemStack.EMPTY
        var container = ItemStack.EMPTY

        for (i in 0..<pContainer.containerSize) {
            val stack = pContainer.getItem(i)
            if (!stack.isEmpty) {
                if (stack.`is`(ModItems.VEHICLE_RESET_KIT.get())) {
                    if (!kit.isEmpty) {
                        return false
                    }
                    kit = stack
                } else if (stack.`is`(ModItems.CONTAINER.get())) {
                    if (!container.isEmpty) {
                        return false
                    }
                    container = stack
                }
            }
        }
        return !kit.isEmpty && !container.isEmpty
    }

    override fun assemble(pContainer: CraftingContainer, pRegistryAccess: RegistryAccess): ItemStack {
        var kit = ItemStack.EMPTY
        var container = ItemStack.EMPTY

        for (i in 0..<pContainer.containerSize) {
            val stack = pContainer.getItem(i)
            if (!stack.isEmpty) {
                if (stack.`is`(ModItems.VEHICLE_RESET_KIT.get())) {
                    if (!kit.isEmpty) {
                        return ItemStack.EMPTY
                    }
                    kit = stack.copy()
                } else if (stack.`is`(ModItems.CONTAINER.get())) {
                    if (!container.isEmpty) {
                        return ItemStack.EMPTY
                    }
                    container = stack.copy()
                }
            }
        }

        if (!kit.isEmpty && !container.isEmpty) {
            val tag = BlockItem.getBlockEntityData(container)
            if (tag != null) {
                val type = tag.getString("EntityType")
                val entityType = EntityType.byString(type).orElse(null)
                if (entityType != null) {
                    return ContainerBlockItem.createInstance(entityType)
                }
            }
        }
        return ItemStack.EMPTY
    }

    override fun canCraftInDimensions(pWidth: Int, pHeight: Int): Boolean {
        return pWidth * pHeight >= 2
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return ModRecipes.VEHICLE_RESET_SERIALIZER.get()
    }
}
