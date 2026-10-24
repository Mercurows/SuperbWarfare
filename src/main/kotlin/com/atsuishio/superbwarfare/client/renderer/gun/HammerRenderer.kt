package com.atsuishio.superbwarfare.client.renderer.gun

import net.minecraft.world.item.ItemStack

class HammerRenderer : GeoGunRenderer() {
    override fun scriptBipodProgress(stack: ItemStack): Double = 1.0
}
