package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.particle.CustomSmokeOption
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

open class SmokeDecoyEntity : Entity {
    var life: Int = 400
    var igniteTime: Int = 4
    var releaseSmoke: Boolean = true

    constructor(type: EntityType<out SmokeDecoyEntity>, level: Level) : super(type, level)

    constructor(type: EntityType<out SmokeDecoyEntity>, level: Level, release: Boolean) : super(type, level) {
        releaseSmoke = release
    }

    constructor(level: Level) : super(ModEntities.SMOKE_DECOY.get(), level)

    override fun readAdditionalSaveData(compoundTag: CompoundTag) {
        if (compoundTag.contains("IgniteTime")) {
            this.igniteTime = compoundTag.getInt("IgniteTime")
        }
        if (compoundTag.contains("Life")) {
            this.life = compoundTag.getInt("Life")
        }
    }

    override fun addAdditionalSaveData(compoundTag: CompoundTag) {
        compoundTag.putInt("IgniteTime", igniteTime)
        compoundTag.putInt("Life", life)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {}

    override fun tick() {
        super.tick()
        this.move(MoverType.SELF, this.deltaMovement)
        if (tickCount == this.igniteTime) {
            if (releaseSmoke) {
                val level = this.level()
                if (level is ServerLevel) {
                    ParticleTool.sendParticle(
                        level, CustomSmokeOption(1f, 1f, 1f), this.xo, this.yo, this.zo,
                        50, 0.0, 0.0, 0.0, 0.07, true
                    )
                    ParticleTool.sendParticle(
                        level,
                        ParticleTypes.LARGE_SMOKE,
                        this.xo,
                        this.yo,
                        this.zo,
                        10,
                        1.0,
                        1.0,
                        1.0,
                        0.1,
                        true
                    )
                    ParticleTool.sendParticle(
                        level,
                        ModParticleTypes.FIRE_STAR.get(),
                        this.xo,
                        this.yo,
                        this.zo,
                        30,
                        0.0,
                        0.0,
                        0.0,
                        0.2,
                        true
                    )
                }
                level.playSound(
                    null,
                    this,
                    ModSounds.SMOKE_FIRE.get(),
                    this.soundSource,
                    2f,
                    random.nextFloat() * 0.05f + 1
                )
            }
            this.deltaMovement = Vec3.ZERO
        }

        if (this.tickCount > this.life) {
            this.discard()
        }
    }

    fun decoyShoot(entity: Entity, shootVec: Vec3, pVelocity: Float, pInaccuracy: Float) {
        val vec3 = shootVec.normalize().add(
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble())
        ).scale(pVelocity.toDouble())
        this.deltaMovement = entity.deltaMovement.scale(0.75).add(vec3)
        val d0 = vec3.horizontalDistance()
        this.yRot = (Mth.atan2(vec3.x, vec3.z) * 57.2957763671875).toFloat()
        this.xRot = (Mth.atan2(vec3.y, d0) * 57.2957763671875).toFloat()
        this.yRotO = this.yRot
        this.xRotO = this.xRot
    }
}
