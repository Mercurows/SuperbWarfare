package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class Mk82Entity(type: EntityType<out Mk82Entity>, level: Level) : AerialBombEntity(type, level),
    BasicGeoProjectileEntity {
    val anim: BasicProjectileAnimationInstance<*>? =
        if (this.level().isClientSide) BasicProjectileAnimationInstance(this) else null

    init {
        this.noCulling = true
        this.explosionRadiusValue = 22f
        this.explosionDamageValue = 650f
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val entity = source.directEntity
        if (entity is Mk82Entity && entity.owner == this.owner) {
            return false
        }

        return super.hurt(source, amount)
    }

    override val maxHealth: Float
        get() = 50f

    override fun getAnimationInstance(): BasicProjectileAnimationInstance<*>? {
        return this.anim
    }
}
