package com.atsuishio.superbwarfare.client.tooltip.component

import net.minecraft.world.item.ItemStack

class BocekImageComponent(width: Int, height: Int, stack: ItemStack) : GunImageComponent(width, height, stack) {
    constructor(stack: ItemStack) : this(32, 16, stack)
}