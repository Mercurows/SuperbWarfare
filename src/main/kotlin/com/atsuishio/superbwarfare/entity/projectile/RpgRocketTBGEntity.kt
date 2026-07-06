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

open class RpgRocketTBGEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    init {
        this.durability = 20
        this.gravityValue = 0.03f
    }

    constructor(type: EntityType<out RpgRocketTBGEntity>, level: Level) : super(type, level) {
        this.damageValue = 250f
        this.explosionDamageValue = 200f
        this.explosionRadiusValue = 10f
    }

    constructor(
        pEntityType: EntityType<out RpgRocketTBGEntity>,
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
        return ModItems.RPG_ROCKET_TBG.get()
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

    override fun getHiddenTicks() = 1
}
