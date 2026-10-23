package com.atsuishio.superbwarfare.client.renderer.gun

import net.minecraft.world.item.ItemStack

class KnifeRenderer : GeoGunRenderer() {
    override fun scriptBipodProgress(stack: ItemStack): Double = 1.0
}
