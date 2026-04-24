package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitEntity
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeProjectileHitDamage
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.VectorTool.calculateAngle
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.max

open class JavelinMissileEntity : MissileProjectile, BasicGeoProjectileEntity {

    var isTop by TOP

    constructor(type: EntityType<out JavelinMissileEntity>, level: Level) : super(type, level) {
        this.noCulling = true
    }

    constructor(
        entity: Entity?,
        level: Level,
        damage: Float,
        explosionDamage: Float,
        explosionRadius: Float,
        guideType: Int,
        targetPos: Vec3?
    ) : super(
        ModEntities.JAVELIN_MISSILE.get(), entity, level
    ) {
        this.noCulling = true
        this.damageValue = damage
        this.explosionDamageValue = explosionDamage
        this.explosionRadiusValue = explosionRadius
        this.guideType = guideType
        this.durability = 50
        if (targetPos != null) {
            this.targetPos = targetPos
        }
    }

    override fun getDefaultItem(): Item {
        return ModItems.JAVELIN_MISSILE.get()
    }

    fun setAttackMode(mode: Boolean) {
        this.isTop = mode
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(TOP, false)
    }

    override fun onHitEntity(result: EntityHitResult) {
        val entity = result.entity
        val owner = this.owner
        if (owner != null && owner.vehicle != null && entity == owner.vehicle) return
        if (this.level() is ServerLevel) {
            entity.forceHurt(
                causeProjectileHitDamage(this.level().registryAccess(), this, owner),
                this.damageValue * if (this.isTop) 1.25f else 1f
            )

            if (entity is LivingEntity) {
                entity.invulnerableTime = 0
            }

            causeExplode(result.getLocation())
            this.discard()
        }

        postEvent(
            HitEntity(
                owner,
                this,
                entity,
                result.getLocation()
            )
        )
    }

    override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)
        if (this.level() is ServerLevel) {
            destroyBlock(result)
        }
    }

    override fun tick() {
        super.tick()

        mediumTrail()

        val entity = EntityFindUtil.findEntity(this.level(), targetUUID)
        val decoy = SeekTool.seekLivingEntities(this, 32.0, 90.0)

        for (e in decoy) {
            if (e.type.`is`(ModTags.EntityTypes.DECOY) && !this.distracted) {
                this.targetUUID = e.getStringUUID()
                this.distracted = true
                break
            }
        }

        if (guideType == 0 || targetUUID != "none") {
            if (entity != null) {
                val dir = position().vectorTo(entity.position()).horizontalDistanceSqr() < 900
                val dis = entity.position().vectorTo(position()).horizontalDistance()
                val height = if (dis > 30) 0.2 * (dis - 30) else 0.0
                val targetPos = Vec3(
                    entity.x,
                    entity.y + 0.5f * entity.bbHeight + (if (entity is EnderDragon) -3 else 0) + height,
                    entity.z
                )
                val targetVec = Vec3(entity.deltaMovement.x, 0.0, entity.deltaMovement.z)
                val toVec = position().vectorTo(targetPos.add(targetVec)).normalize()
                if ((!entity.getPassengers().isEmpty() || entity is VehicleEntity)
                    && entity.tickCount % (max(0.04 * this.distanceTo(entity), 2.0).toInt()) == 0
                ) {
                    entity.level().playSound(
                        null,
                        entity.onPos,
                        if (entity is Pig) SoundEvents.PIG_HURT else ModSounds.MISSILE_WARNING.get(),
                        SoundSource.PLAYERS,
                        2f,
                        1f
                    )
                }
                if (this.tickCount > 3) {
                    if (isTop) {
                        if (!dir) {
                            val targetTopPos =
                                Vec3(targetPos.x, targetPos.y + (6 * this.tickCount).coerceIn(0, 90), targetPos.z)
                            val toTopVec = position().vectorTo(targetTopPos).normalize()
                            turn(toTopVec, 6f)
                        } else {
                            val lostTarget = this.y < entity.y
                            if (!lostTarget) {
                                turn(toVec, 180f)
                                this.deltaMovement = this.deltaMovement.scale(0.1).add(lookAngle.scale(8.0))
                            }
                        }
                    } else {
                        val lostTarget = (calculateAngle(lookAngle, toVec) > 80)
                        if (!lostTarget) {
                            turn(toVec, 6f)
                        }
                    }
                }
            }
        } else if (guideType == 1 && targetPos != null) {
            val dis = targetPos!!.vectorTo(position()).horizontalDistance()
            val height = if (dis > 30) 0.2 * (dis - 30) else 0.0
            val dir = position().vectorTo(targetPos!!).horizontalDistanceSqr() < 900
            val toVec = eyePosition.vectorTo(targetPos!!.add(0.0, height, 0.0)).normalize()

            if (this.tickCount > 3) {
                if (isTop) {
                    if (!dir) {
                        val targetTopPos =
                            Vec3(targetPos!!.x, targetPos!!.y + (5 * this.tickCount).coerceIn(0, 90), targetPos!!.z)
                        val toTopVec = eyePosition.vectorTo(targetTopPos).normalize()
                        turn(toTopVec, 6f)
                    } else {
                        val lostTarget = this.y < targetPos!!.y
                        if (!lostTarget) {
                            turn(toVec, 180f)
                            this.deltaMovement = this.deltaMovement.scale(0.1).add(lookAngle.scale(8.0))
                        }
                    }
                } else {
                    val lostTarget = deltaMovement.angleTo(toVec) > 80
                    if (!lostTarget) {
                        turn(toVec, 6f)
                    }
                }
            }
        }

        if (this.tickCount > 3) {
            this.deltaMovement = this.deltaMovement.add(lookAngle)
        }

        this.deltaMovement = this.deltaMovement.multiply(0.8, 0.8, 0.8)
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.4f
    }

    override fun getModel() = BedrockModelLoader.JAVELIN_MISSILE_MODEL

    companion object {
        @JvmField
        val TOP: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(JavelinMissileEntity::class.java, EntityDataSerializers.BOOLEAN)
    }
}
