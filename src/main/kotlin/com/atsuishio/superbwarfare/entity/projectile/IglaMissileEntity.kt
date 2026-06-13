package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
import com.atsuishio.superbwarfare.tools.VectorTool.checkNoClip
import com.atsuishio.superbwarfare.tools.angleTo
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.max

open class IglaMissileEntity : MissileProjectile, BasicGeoProjectileEntity {

    constructor(type: EntityType<out IglaMissileEntity>, level: Level) : super(type, level)

    constructor(entity: Entity?, level: Level, damage: Float, explosionDamage: Float, explosionRadius: Float) : super(
        ModEntities.IGLA_MISSILE.get(), entity, level
    ) {
        this.damageValue = damage
        this.explosionDamageValue = explosionDamage
        this.explosionRadiusValue = explosionRadius
        this.durability = 0
    }

    override fun getDefaultItem(): Item {
        return ModItems.MEDIUM_ANTI_AIR_MISSILE.get()
    }

    override fun tick() {
        super.tick()

        mediumTrail()

        val entity = EntityFindUtil.findEntity(this.level(), this.getTargetUUID())

        if (entity != null && this.getTargetUUID() != "none") {
            if ((entity.getPassengers().isNotEmpty() || entity is VehicleEntity)
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

            val targetPos = Vec3(
                entity.x,
                entity.y + 0.5f * entity.bbHeight + (if (entity is EnderDragon) -3 else 0),
                entity.z
            )
            val toVec = calculateFiringSolution(
                position(),
                targetPos,
                entity.deltaMovement,
                deltaMovement.length(),
                0.0
            )

            if (this.tickCount > 1) {
                setLostTarget(deltaMovement.angleTo(toVec) > 120 && !isLostTarget())

                val owner = this.owner
                if (owner is Player && owner.mainHandItem.`is`(ModItems.IGLA_9K38.get()) && !isLost()) {
                    val handItem = owner.mainHandItem
                    val data = from(handItem)
                    setLost(!data.zooming.get() || !checkNoClip(owner.eyePosition, targetPos, this.level()))
                }

                if (!isLostTarget() && !isLost()) {
                    turn(toVec, ((tickCount - 1) * 0.5f).coerceIn(0f, 15f))
                    this.deltaMovement = this.deltaMovement.scale(0.05).add(lookAngle.scale(8.0))
                }

                if (isLostTarget()) {
                    this.setTargetUUID("none")
                }
            }
        }

        if (isLost()) {
            deltaMovement = deltaMovement.add(0.0, 0.03, 0.0)
            this.setTargetUUID("none")
        }
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.4f
    }
}
