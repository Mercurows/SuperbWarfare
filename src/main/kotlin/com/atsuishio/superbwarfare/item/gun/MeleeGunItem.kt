package com.atsuishio.superbwarfare.item.gun

import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer
import com.atsuishio.superbwarfare.client.renderer.gun.KnifeRenderer
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

open class MeleeGunItem(properties: Properties) : GeoGunItem(properties) {

    override fun isDamageable(stack: ItemStack?): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun createRenderer(): GeoGunRenderer = KnifeRenderer()
}
