package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import net.minecraft.resources.ResourceLocation

interface BasicGeoProjectileEntity {
    val model: ResourceLocation
    val animation: ResourceLocation?
        get() = null
    val animationInstance: BasicProjectileAnimationInstance<*>?
        get() = null
    val emissiveTexture: ResourceLocation?
        get() = null
}