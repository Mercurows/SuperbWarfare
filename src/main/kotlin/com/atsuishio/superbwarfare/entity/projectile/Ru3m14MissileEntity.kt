package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.VectorTool
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

open class Ru3m14MissileEntity(type: EntityType<out Ru3m14MissileEntity>, level: Level) :
    MissileProjectile(type, level),
    BasicGeoProjectileEntity {
    val anim: BasicProjectileAnimationInstance<*>? =
        if (this.level().isClientSide) BasicProjectileAnimationInstance(this) else null

    var startPos: Vec3 = Vec3.ZERO
    var distance = 0.0

    override fun getAnimationInstance(): BasicProjectileAnimationInstance<*>? {
        return this.anim
    }

    init {
        this.damageValue = 3000f
        this.explosionDamageValue = 1400f
        this.explosionRadiusValue = 36f
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val entity = source.directEntity
        if (entity is Ru3m14MissileEntity && entity.owner == this.owner) {
            return false
        }

        return super.hurt(source, amount)
    }

    override fun getDefaultItem(): Item {
        return ModItems.EXTRA_LARGE_ANTI_GROUND_MISSILE.get()
    }

    override fun tick() {
        super.tick()

        val level = this.level()
        var toVec = lookAngle

        if (targetPos != null && level is ServerLevel) {
            if (tickCount == 1) {
                startPos = position()
                distance = targetPos!!.vectorTo(position()).horizontalDistance()
            }
            val flyDistance = position().vectorTo(startPos).horizontalDistance()
            val dis = Mth.clamp(flyDistance / distance, 0.0, 1.0)

            var height: Double = if (dis < 0.2) {
                0.5 * flyDistance
            } else if (dis >= 0.2 && dis < 0.8) {
                0.1 * distance
            } else {
                0.0
            }

            val d = targetPos!!.vectorTo(position()).horizontalDistance()

            if (d < 600) {
                height = 0.125 * d
            }

            if (d < 100) {
                height = 0.0
            }

            var targetPos = this.targetPos!!.add(0.0, height, 0.0)
            if (targetPos.y > 2048) {
                targetPos = Vec3(targetPos.x, 2048.0, targetPos.z)
            }
            toVec = position().vectorTo(targetPos)
        }

        if (targetPos == null && tickCount > 200) {
            discard()
            causeExplode(position())
        }

        if (tickCount in 2..10 && toVec != lookAngle) {
            turnYaw(toVec, 30f)
        }

        if (this.tickCount > 10) {
            hugeMissileTrail()
            if (level is ServerLevel) {
                val lostTarget = (VectorTool.calculateAngle(lookAngle, toVec) > 90 && tickCount > 50)

                this.deltaMovement =
                    this.deltaMovement.add(lookAngle.scale(Mth.clamp(0.05 * (tickCount - 10), 0.15, 1.5)))
                val f = (0.84 + y * 0.00005).coerceAtMost(0.86)
                this.deltaMovement = this.deltaMovement.multiply(f, f, f)

                if (!lostTarget) {
                    turn(toVec, ((tickCount - 10) * 0.1f).coerceIn(0f, 30f))
                }
            }
        } else {
            this.deltaMovement = this.deltaMovement.add(0.0, -0.1, 0.0)
            this.deltaMovement = this.deltaMovement.multiply(0.99, 0.99, 0.99)
        }

        if (this.tickCount == 8) {
            level.playSound(
                null,
                BlockPos.containing(position()),
                ModSounds.MISSILE_START.get(),
                SoundSource.PLAYERS,
                4f,
                1f
            )
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
    }

    override fun getDefaultGravity(): Double {
        return if (tickCount < 8) 0.1 else super.getDefaultGravity()
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override val maxHealth: Float
        get() = 200f

    override fun getFlareHiddenTicks(): Int {
        return 9
    }
}
