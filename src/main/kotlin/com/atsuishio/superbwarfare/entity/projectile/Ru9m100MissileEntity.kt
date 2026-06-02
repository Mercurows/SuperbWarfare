package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.*
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

open class Ru9m100MissileEntity(type: EntityType<out Ru9m100MissileEntity>, level: Level) :
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

        val entity = EntityFindUtil.findEntity(this.level(), this.targetUUID)
        val decoy = SeekTool.seekLivingEntities(this, 32.0, 90.0)
        val level = this.level()

        for (e in decoy) {
            if (e.type.`is`(ModTags.EntityTypes.DECOY) && !this.distracted) {
                this.targetUUID = e.stringUUID
                this.distracted = true
                break
            }
        }

        if (entity != null && this.targetUUID != "none") {
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
            val toVec = RangeTool.calculateFiringSolution(
                position(),
                targetPos,
                entity.deltaMovement,
                deltaMovement.length(),
                0.0
            )

            if (tickCount in 2..10) {
                turnYaw(toVec, 30f)
            }

            if (this.tickCount > 10) {
                largeTrail()

                if (this.tickCount > 20 && !lostTarget) {
                    lostTarget = VectorTool.calculateAngle(deltaMovement, toVec) > 120
                }

                if (!lostTarget) {
                    turn(toVec, ((tickCount - 1) * 0.5f).coerceIn(0f, 15f))
                    this.deltaMovement = this.deltaMovement.scale(0.05).add(lookAngle.scale(8.0))
                }

                if (lostTarget) {
                    this.targetUUID = "none"
                }
            }
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
        return if (tickCount < 8) 0.15 else super.getDefaultGravity()
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override val maxHealth: Float
        get() = 50f

    override fun getFlareHiddenTicks(): Int {
        return 9
    }
}
