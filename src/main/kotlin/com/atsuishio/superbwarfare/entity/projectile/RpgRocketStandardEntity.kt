package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult

open class RpgRocketStandardEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    init {
        this.durability = 50
        this.gravityValue = 0.015f
    }

    constructor(type: EntityType<out RpgRocketStandardEntity>, level: Level) : super(type, level) {
        this.damageValue = 340f
        this.explosionDamageValue = 80f
        this.explosionRadiusValue = 5f
    }

    constructor(
        pEntityType: EntityType<out RpgRocketStandardEntity>,
        pX: Double,
        pY: Double,
        pZ: Double,
        pLevel: Level,
        damage: Float,
        explosionDamage: Float,
        explosionRadius: Float
    ) : super(pEntityType, pX, pY, pZ, pLevel) {
        this.damageValue = damage
        this.explosionDamageValue = explosionDamage
        this.explosionRadiusValue = explosionRadius
    }

    override fun afterHitBlock(result: BlockHitResult) {
        if (this.level() is ServerLevel) {
            destroyBlock(result)
        }
    }

    override fun getDefaultItem(): Item {
        return ModItems.RPG_ROCKET_STANDARD.get()
    }

    override fun tick() {
        super.tick()
        mediumTrail()

        if (this.tickCount == 3) {
            val level = this.level()
            if (level is ServerLevel) {
                ParticleTool.sendParticle(
                    level,
                    ParticleTypes.CLOUD,
                    this.xo,
                    this.yo,
                    this.zo,
                    15,
                    0.8,
                    0.8,
                    0.8,
                    0.01,
                    true
                )
                ParticleTool.sendParticle(
                    level,
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.xo,
                    this.yo,
                    this.zo,
                    10,
                    0.8,
                    0.8,
                    0.8,
                    0.01,
                    true
                )
            }
        }
        if (this.tickCount > 2) {
            this.deltaMovement = this.deltaMovement.multiply(1.03, 1.03, 1.03)
        }
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.2f
    }

    override fun getHiddenTicks() = 1
}
