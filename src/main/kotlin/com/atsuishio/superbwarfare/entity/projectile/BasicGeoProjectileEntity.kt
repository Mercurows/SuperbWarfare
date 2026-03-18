package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import net.minecraft.resources.ResourceLocation

interface BasicGeoProjectileEntity {
    fun getModel(): ResourceLocation

    fun getAnimation(): ResourceLocation? = null

    fun getAnimationInstance(): BasicProjectileAnimationInstance<*>? = null

    fun getEmissiveTexture(): ResourceLocation? = null

    fun getHiddenTicks(): Int = 0

    fun getFlareHiddenTicks(): Int = 3
}