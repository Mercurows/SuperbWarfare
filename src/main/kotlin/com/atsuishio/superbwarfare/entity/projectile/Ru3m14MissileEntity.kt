package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
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

open class Ru3m14MissileEntity(type: EntityType<out Ru3m14MissileEntity>, level: Level) : MissileProjectile(type, level),
    BasicGeoProjectileEntity {
    val anim: BasicProjectileAnimationInstance<*>? =
        if (this.level().isClientSide) BasicProjectileAnimationInstance(this) else null

    override fun getAnimationInstance(): BasicProjectileAnimationInstance<*>? {
        return this.anim
    }
    init {
        this.noCulling = true
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
        return ModItems.LARGE_ANTI_GROUND_MISSILE.get()
    }

    override fun tick() {
        super.tick()

        var toVec = lookAngle
        val level = this.level()

        if (level is ServerLevel && targetPos != null) {
            val dis = targetPos!!.vectorTo(position()).horizontalDistance()
            val height = if (dis > 30) 0.4 * (dis - 30) else 0.0
            val targetPos = this.targetPos!!.add(0.0, height, 0.0)
            toVec = calculateFiringSolution(position(), targetPos, Vec3.ZERO, deltaMovement.length(), 0.0)
        }

        if (tickCount in 2..10 && toVec != lookAngle) {
            turnYaw(toVec, 30f)
        }

        if (this.tickCount > 10) {
            largeTrail()
            this.deltaMovement = this.deltaMovement.scale(0.05).add(lookAngle.scale(8.0))
            this.deltaMovement = this.deltaMovement.multiply(0.85, 0.85, 0.85)
            turn(toVec, ((tickCount - 10) * 0.5f).coerceIn(0f, 15f))
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

    fun turnYaw(vec3: Vec3, turnSpeed: Float) {
        var vec3 = vec3
        val v0 = deltaMovement.normalize()

        vec3 = vec3.add(v0.scale(-0.4))

        val targetAngleY = (-Mth.atan2(vec3.x, vec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        val diffY = Mth.wrapDegrees(targetAngleY - this.yRot)

        deltaMovement = deltaMovement.scale(1 - 0.0004 * VehicleVecUtils.calculateAngle(vec3, v0))
        this.yRot += (0.95f * diffY).coerceIn(-turnSpeed, turnSpeed)

    }

    override fun getGravity(): Float {
        return if (tickCount < 8) 0.15f else super.getGravity()
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.7f
    }

    override val maxHealth: Float
        get() = 200f

    override fun getFlareHiddenTicks(): Int {
        return 19
    }
}
