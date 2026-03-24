package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.VectorTool.calculateAngle
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

open class Ru9m336MissileEntity(type: EntityType<out Ru9m336MissileEntity>, level: Level) :
    MissileProjectile(type, level),
    BasicGeoProjectileEntity {

    init {
        this.noCulling = true
    }

    override fun getDefaultItem(): Item {
        return ModItems.MEDIUM_ANTI_AIR_MISSILE.get()
    }

    override fun tick() {
        super.tick()

        mediumTrail()

        val entity = EntityFindUtil.findEntity(this.level(), this.targetUUID)
        val decoy = SeekTool.seekLivingEntities(this, 32.0, 90.0)

        for (e in decoy) {
            if (e.type.`is`(ModTags.EntityTypes.DECOY) && !this.distracted) {
                this.targetUUID = e.getStringUUID()
                this.distracted = true
                break
            }
        }

        if (entity != null && this.targetUUID != "none") {
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
                lostTarget = calculateAngle(deltaMovement, toVec) > 120 && !lostTarget

                if (!lostTarget) {
                    turn(toVec, ((tickCount - 1) * 0.5f).coerceIn(0f, 15f))
                    this.deltaMovement = this.deltaMovement.scale(0.05).add(lookAngle.scale(8.0))
                }

                if (lostTarget) {
                    this.targetUUID = "none"
                }
            }
        }
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.4f
    }

    override fun getModel() = BedrockModelLoader.RU_9M336_MISSILE_MODEL
}
