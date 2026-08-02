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
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

open class Ru3m14MissileEntity(type: EntityType<out Ru3m14MissileEntity>, level: Level) :
    MissileProjectile(type, level),
    BasicGeoProjectileEntity {
    val anim: BasicProjectileAnimationInstance<*>? =
        if (this.level().isClientSide) BasicProjectileAnimationInstance(this) else null

    override fun getAnimationInstance(): BasicProjectileAnimationInstance<*>? {
        return this.anim
    }

    init {
        this.damageValue = 3000f
        this.explosionDamageValue = 1400f
        this.explosionRadiusValue = 36f
    }

    override fun getDefaultItem(): Item {
        return ModItems.EXTRA_LARGE_ANTI_GROUND_MISSILE.get()
    }

    override fun tick() {
        super.tick()

        val level = this.level()
        var toVec = lookAngle

        if (getTargetPos() != null && level is ServerLevel) {
            val targetPos = this.getTargetPos()!!
            val d = targetPos.vectorTo(position()).horizontalDistance()

            toVec = if (tickCount <= 10) {
                // 点火阶段：先水平对准目标方向
                position().vectorTo(targetPos)
            } else if (d < 1400) {
                // 末端冲刺段：水平距离目标小于 1400m，径直飞向目标
                position().vectorTo(targetPos)
            } else if (y < 1024) {
                // 爬升段：点火后尽可能爬高到 1024m 高度
                position().vectorTo(targetPos).multiply(1.0, 0.0, 1.0).normalize().scale((d * 0.1).coerceAtMost(4096.0)).add(position().vectorTo(Vec3(x, 1024.0, z)))
            } else {
                // 巡航段：在 1024m 高空平飞逼近目标
                position().vectorTo(Vec3(targetPos.x, 1024.0, targetPos.z))
            }
        }

        if (getTargetPos() == null && tickCount > 200 && level() is ServerLevel) {
            discard()
            causeExplode(position())
        }

        if (tickCount in 2..10 && toVec != lookAngle) {
            turnYaw(toVec, 30f)
        }

        if (this.tickCount > 10) {
            hugeMissileTrail()
            if (level is ServerLevel) {
                val lostTarget = (VectorTool.calculateAngle(lookAngle, toVec) > 60 && tickCount > 50)

                this.deltaMovement =
                    this.deltaMovement.add(lookAngle.scale(Mth.clamp(0.06 * (tickCount - 10), 0.15, 2.0)))

                val f = (0.85 + y * 0.000075).coerceAtMost(0.92)
                this.deltaMovement = this.deltaMovement.multiply(f, f, f)

                if (!lostTarget) {
                    val d = if (getTargetPos() != null) {
                        getTargetPos()!!.vectorTo(position()).horizontalDistance()
                    } else {
                        Double.MAX_VALUE
                    }

                    if (getTargetPos() != null && d < 1024) {
                        // 末端冲刺：径直向目标加速
                        toVec = position().vectorTo(getTargetPos()!!)
                        this.deltaMovement = this.deltaMovement.multiply(1.01, 1.01, 1.01).add(toVec.normalize().scale(2.0))
                        turn(toVec, 90f)
                    } else {
                        turn(toVec, ((tickCount - 10) * 0.1f).coerceIn(0f, 30f))
                    }
                } else {
                    lostTargetTick++
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

    override fun getCustomGravity(): Float {
        return if (tickCount < 8) 0.1f else super.getCustomGravity()
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override val maxHealth: Float
        get() = 200f

    override fun getFlareHiddenTicks(): Int {
        return 9
    }

    override fun getNoHitTicks(): Int {
        return 9
    }
}
