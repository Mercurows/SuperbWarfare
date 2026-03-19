package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModDamageTypes.causeProjectileHitDamage
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult

open class RpgRocketTBGEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    constructor(type: EntityType<out RpgRocketTBGEntity>, level: Level) : super(type, level) {
        this.noCulling = true
        this.durability = 20
        this.damageValue = 250f
        this.explosionDamageValue = 200f
        this.explosionRadiusValue = 10f
        this.gravityValue = 0.03f
    }

    constructor(
        pEntityType: EntityType<out ThrowableItemProjectile>,
        pX: Double,
        pY: Double,
        pZ: Double,
        pLevel: Level,
        damage: Float,
        explosionDamage: Float,
        explosionRadius: Float
    ) : super(pEntityType, pX, pY, pZ, pLevel) {
        this.noCulling = true
        this.durability = 20
        this.damageValue = damage
        this.explosionDamageValue = explosionDamage
        this.explosionRadiusValue = explosionRadius
        this.gravityValue = 0.03f
    }

    override fun getDefaultItem(): Item {
        return ModItems.RPG_ROCKET_TBG.get()
    }

    public override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)
        if (this.level() is ServerLevel) {
            destroyBlock(result)
        }
    }

    override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        val entity = result.entity
        val owner = this.owner
        if (owner != null && owner.vehicle != null && entity == owner.vehicle) return
        if (this.level() is ServerLevel) {
            entity.forceHurt(
                causeProjectileHitDamage(this.level().registryAccess(), this, owner),
                this.damageValue
            )

            if (entity is LivingEntity) {
                entity.invulnerableTime = 0
            }

            causeExplode(result.getLocation())
            this.discard()
        }
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

    override fun getModel() = BedrockModelLoader.RPG_ROCKET_TBG_MODEL

    override fun getHiddenTicks() = 1
}
