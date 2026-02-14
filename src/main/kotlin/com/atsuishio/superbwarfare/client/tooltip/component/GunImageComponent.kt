package com.atsuishio.superbwarfare.client.tooltip.component

import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.ItemStack

open class GunImageComponent(var width: Int, var height: Int, var stack: ItemStack) :
    TooltipComponent {
    constructor(stack: ItemStack) : this(32, 16, stack)
}