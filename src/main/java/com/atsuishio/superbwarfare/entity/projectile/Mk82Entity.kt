package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class Mk82Entity(type: EntityType<out Mk82Entity>, level: Level) : AerialBombEntity(type, level),
    BasicGeoProjectileEntity {
    val anim: BasicProjectileAnimationInstance<*>? =
        if (this.level().isClientSide) BasicProjectileAnimationInstance(this) else null

    init {
        this.noCulling = true
        this.explosionRadius = 22f
        this.explosionDamage = 650f
    }

    override val maxHealth: Float
        get() = 50f

    override fun getModel() = BedrockModelLoader.MK_82_MODEL

    override fun getAnimation() = BedrockModelLoader.MK_82_ANI

    override fun getAnimationInstance(): BasicProjectileAnimationInstance<*>? {
        return this.anim
    }
}
