package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import net.minecraft.resources.ResourceLocation

interface BasicGeoVehicleEntity {
    fun getAnimation(): ResourceLocation? = null

    fun getAnimationInstance(): BasicProjectileAnimationInstance<*>? = null

    fun getEmissiveTexture(): ResourceLocation? = null
}