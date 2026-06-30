package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.RangeTool
import com.atsuishio.superbwarfare.tools.VectorTool
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
        return ModItems.LARGE_ANTI_AIR_MISSILE.get()
    }

    override fun tick() {
        super.tick()

        val entity = EntityFindUtil.findEntity(this.level(), this.getTargetUUID())
        val level = this.level()

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

                if (this.tickCount > 20 && !isLostTarget()) {
                    setLostTarget(VectorTool.calculateAngle(deltaMovement, toVec) > 120)
                }

                if (!isLostTarget()) {
                    turn(toVec, ((tickCount - 1) * 0.5f).coerceIn(0f, 15f))
                    this.deltaMovement = this.deltaMovement.scale(0.05).add(lookAngle.scale(((tickCount - 10) * 0.2).coerceIn(0.0, 40.0)))
                }

                if (isLostTarget()) {
                    this.setTargetUUID("none")
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

        if (this.tickCount > 10) {
            hugeMissileTrail()
        }
    }

    override fun getCustomGravity(): Float {
        return if (tickCount < 8) 0.1f else super.getCustomGravity()
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override val maxHealth: Float
        get() = 50f

    override fun getFlareHiddenTicks(): Int {
        return 9
    }

    override fun getNoHitTicks(): Int {
        return 9
    }
}
