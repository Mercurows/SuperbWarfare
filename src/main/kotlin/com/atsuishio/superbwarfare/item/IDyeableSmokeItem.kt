package com.atsuishio.superbwarfare.item

import net.minecraft.world.item.ItemStack

interface IDyeableSmokeItem {
    fun setColor(stack: ItemStack, color: Int)

    fun getColor(stack: ItemStack): Int

    companion object {
        const val TAG_COLOR: String = "Color"
    }
}