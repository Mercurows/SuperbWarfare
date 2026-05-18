package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import net.minecraft.resources.ResourceLocation

interface BasicGeoProjectileEntity {
    @Deprecated("Model location is auto loaded now")
    fun getModel(): ResourceLocation = loc("projectile/projectile")

    @Deprecated("Animation location is auto loaded now")
    fun getAnimation(): ResourceLocation? = null

    fun getAnimationInstance(): BasicProjectileAnimationInstance<*>? = null

    fun getEmissiveTexture(): ResourceLocation? = null

    fun getHiddenTicks(): Int = 0

    fun getFlareHiddenTicks(): Int = 3
}