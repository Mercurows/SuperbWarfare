package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

// TODO 动画怎么不会正常播放？
open class Mk82Entity(type: EntityType<out Mk82Entity>, level: Level) : AerialBombEntity(type, level),
    BasicGeoProjectileEntity {
    init {
        this.noCulling = true
        this.explosionRadius = 22f
        this.explosionDamage = 650f
    }

    override val maxHealth: Float
        get() = 50f

    override val model: ResourceLocation
        get() = BedrockModelLoader.MK_82_MODEL
    override val animation: ResourceLocation?
        get() = BedrockModelLoader.MK_82_ANI
    override val animationInstance: BasicProjectileAnimationInstance<*>?
        get() = if (this.level().isClientSide) BasicProjectileAnimationInstance(this) else null
}
