package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.item.projectile.SeaMineItem
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.level.Level
import net.minecraftforge.common.Tags

class SeaMineUpgradeRecipe(id: ResourceLocation, category: CraftingBookCategory) : CustomRecipe(id, category) {
    private fun result(grid: CraftingContainer): ItemStack {
        var mine = ItemStack.EMPTY
        var mineSlot = -1
        var iron = 0
        var chains = 0
        for (i in 0 until grid.containerSize) {
            val stack = grid.getItem(i)
            if (stack.isEmpty) continue
            when {
                stack.item is SeaMineItem -> {
                    if (!mine.isEmpty) return ItemStack.EMPTY
                    mine = stack
                    mineSlot = i
                }

                stack.`is`(Tags.Items.INGOTS_IRON) -> iron++
                stack.`is`(Items.CHAIN) -> chains++
                else -> return ItemStack.EMPTY
            }
        }
        if (mine.isEmpty || iron + chains == 0) return ItemStack.EMPTY
        val weighted = SeaMineItem.hasBallast(mine)
        val currentLength = SeaMineItem.chainLength(mine)
        if (iron > 0) {
            if (weighted || chains > 0 || iron != 4 || grid.width != 3 || grid.height != 3 || mineSlot != 4) {
                return ItemStack.EMPTY
            }
            if (listOf(1, 3, 5, 7).any { !grid.getItem(it).`is`(Tags.Items.INGOTS_IRON) }) return ItemStack.EMPTY
        }
        if (chains > 0 && weighted && currentLength == 0) return ItemStack.EMPTY
        val length = currentLength + chains
        if (length > SeaMineItem.MAX_CHAIN_LENGTH) return ItemStack.EMPTY
        return mine.copyWithCount(1).apply {
            orCreateTag.putBoolean(SeaMineItem.BALLAST, weighted || iron > 0 || length > 0)
            if (length > 0) orCreateTag.putInt(SeaMineItem.CHAIN_LENGTH, length)
        }
    }

    override fun matches(grid: CraftingContainer, level: Level) = !result(grid).isEmpty
    override fun assemble(grid: CraftingContainer, access: RegistryAccess) = result(grid)
    override fun canCraftInDimensions(width: Int, height: Int) = width * height >= 2
    override fun getSerializer() = ModRecipes.SEA_MINE_UPGRADE_SERIALIZER.get()
}
