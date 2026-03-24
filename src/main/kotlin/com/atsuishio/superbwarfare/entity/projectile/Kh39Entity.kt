package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.VectorTool.calculateAngle
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.max

open class Kh39Entity(type: EntityType<out Kh39Entity>, level: Level) : MissileProjectile(type, level),
    BasicGeoProjectileEntity {

    init {
        this.noCulling = true
        this.damageValue = 1100f
        this.explosionDamageValue = 180f
        this.explosionRadiusValue = 12f
        this.distracted = false
        this.durability = 25
    }

    override fun getDefaultItem(): Item {
        return ModItems.LARGE_ANTI_GROUND_MISSILE.get()
    }

    override fun tick() {
        super.tick()

        largeTrail()

        val entity = EntityFindUtil.findEntity(this.level(), this.targetUUID)
        val decoy = SeekTool.seekLivingEntities(this, 32.0, 90.0)

        for (e in decoy) {
            if (e.type.`is`(ModTags.EntityTypes.DECOY) && !this.distracted) {
                this.targetUUID = e.getStringUUID()
                this.distracted = true
                break
            }
        }

        var toVec = lookAngle

        val level = this.level()
        if (guideType == 0) {
            if (this.targetUUID != "none") {
                if (entity != null) {
                    if (level is ServerLevel) {
                        if ((!entity.getPassengers().isEmpty() || entity is VehicleEntity)
                            && entity.tickCount % (max(0.04 * this.distanceTo(entity), 2.0).toInt()) == 0
                        ) {
                            level.playSound(
                                null,
                                entity.onPos,
                                if (entity is Pig) SoundEvents.PIG_HURT else ModSounds.MISSILE_WARNING.get(),
                                SoundSource.PLAYERS,
                                2f,
                                1f
                            )
                        }
                        val dis = entity.position().vectorTo(position()).horizontalDistance()
                        val height = if (dis > 30) 0.4 * (dis - 30) else 0.0
                        val targetPos = Vec3(
                            entity.x,
                            entity.y + (if (entity is EnderDragon) -2 else 0) + height,
                            entity.z
                        )
                        toVec = calculateFiringSolution(
                            position(),
                            targetPos,
                            entity.deltaMovement,
                            deltaMovement.length(),
                            0.0
                        )
                    }
                }
            }
        } else {
            if (level() is ServerLevel && targetPos != null) {
                val dis = targetPos!!.vectorTo(position()).horizontalDistance()
                val height = if (dis > 30) 0.4 * (dis - 30) else 0.0
                val targetPos = this.targetPos!!.add(0.0, height, 0.0)
                toVec = calculateFiringSolution(position(), targetPos, Vec3.ZERO, deltaMovement.length(), 0.0)
            }
        }

        if (this.tickCount > 8) {
            this.deltaMovement = this.deltaMovement.scale(0.05).add(lookAngle.scale(8.0))
            this.deltaMovement = this.deltaMovement.multiply(0.85, 0.85, 0.85)
            val lostTarget = (calculateAngle(lookAngle, toVec) > 170)
            if (!lostTarget) {
                turn(toVec, ((tickCount - 8) * 0.5f).coerceIn(0f, 15f))
            }
        } else {
            this.deltaMovement = this.deltaMovement.add(0.0, -0.06, 0.0)
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
        return if (tickCount < 8) 0.15 else super.getCustomGravity().toDouble()
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.7f
    }

    override val maxHealth: Float
        get() = 70f

    override fun getModel() = BedrockModelLoader.KH_39_MODEL
}
