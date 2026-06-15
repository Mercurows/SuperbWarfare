package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.cos

open class SwarmDroneEntity(type: EntityType<out SwarmDroneEntity>, level: Level) : MissileProjectile(type, level),
    BasicGeoProjectileEntity {
    val anim: BasicProjectileAnimationInstance<*>? =
        if (this.level().isClientSide) BasicProjectileAnimationInstance(this, true) else null

    var randomFloat: Float

    init {
        this.noCulling = true
        this.explosionDamageValue = 80f
        this.explosionRadiusValue = 5f
        randomFloat = random.nextFloat()
    }

    override fun getDefaultItem(): Item {
        return ModItems.SWARM_DRONE.get()
    }

    override fun performDamage(
        entity: Entity,
        damage: Float,
        isHeadshot: Boolean
    ) {
    }

    override fun tick() {
        super.tick()
        val entity = EntityFindUtil.findEntity(this.level(), entityData.get(TARGET_UUID))
        if (this.tickCount == 1) {
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

        val owner = this.owner
        if (tickCount > 10 && owner != null) {
            val targetPos: Vec3?
            if (getGuideType() == 0 && entity != null) {
                val targetVec = Vec3(entity.deltaMovement.x, 0.0, entity.deltaMovement.z)
                targetPos = entity.eyePosition.add(targetVec)
                this.setTargetPos(targetPos)
            } else if (this.getTargetPos() != null) {
                targetPos = this.getTargetPos()
            } else {
                val result = owner.level().clip(
                    ClipContext(
                        owner.eyePosition,
                        owner.eyePosition.add(owner.lookAngle.scale(512.0)),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.ANY,
                        owner
                    )
                )
                targetPos = result.getLocation()
            }

            if (tickCount % 5 == 0) {
                randomFloat = 2 * (random.nextFloat() - 0.5f)
            }

            val dis = position().vectorTo(owner.position()).horizontalDistance()
            val dis2 = position().distanceToSqr(targetPos)
            val disShooter = owner.position().vectorTo(targetPos).horizontalDistance()
            val randomPos = cos((dis / disShooter).coerceIn(0.0, 1.0).toFloat() * 1.5f * Mth.PI) * dis * 4 * randomFloat

            val toVec = this.position().vectorTo(targetPos).add(
                Vec3(
                    -randomPos,
                    abs(randomPos.toFloat()) * 0.02,
                    randomPos
                ).scale(1 - Mth.clamp(0.02 * (tickCount - 20), 0.0, 1.0))
            ).normalize()
            turn(toVec, 90f)
            this.deltaMovement = this.deltaMovement.add(position().vectorTo(targetPos).normalize().scale(0.1))

            if (dis2 < 1) {
                if (this.level() is ServerLevel) {
                    causeExplode(position())
                }
                this.discard()
            }

            this.deltaMovement = this.deltaMovement.multiply(0.55, 0.55, 0.55)
        } else {
            this.deltaMovement = this.deltaMovement.multiply(0.97, 0.97, 0.97)
        }

        if (this.tickCount > 13) {
            this.deltaMovement = this.deltaMovement.add(lookAngle)
        }
    }

    override fun getSound(): SoundEvent {
        return ModSounds.DRONE_ENGINE.get()
    }

    override fun getVolume(): Float {
        return 0.6f
    }

    override val maxHealth: Float
        get() = 4f

    fun setRotate(vec3: Vec3) {
        val d0 = vec3.horizontalDistance()
        this.yRot = (-Mth.atan2(vec3.x, vec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        this.xRot = (-Mth.atan2(vec3.y, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        this.yRotO = this.yRot
        this.xRotO = this.xRot
    }

    override fun shoot(pX: Double, pY: Double, pZ: Double, pVelocity: Float, pInaccuracy: Float) {
        val vec3 = Vec3(pX, pY, pZ).normalize().add(
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble())
        ).scale(pVelocity.toDouble())
        this.deltaMovement = vec3
    }

    override fun getAnimationInstance() = this.anim
}
