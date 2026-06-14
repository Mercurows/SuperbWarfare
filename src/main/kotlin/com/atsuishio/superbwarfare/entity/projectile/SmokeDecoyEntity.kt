package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.particle.CustomSmokeOption
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraftforge.network.NetworkHooks

open class SmokeDecoyEntity : Entity {
    var life: Int = 400
    var igniteTime: Int = 4
    var releaseSmoke: Boolean = true
    var red: Float = 1.0f
        private set
    var green: Float = 1.0f
        private set
    var blue: Float = 1.0f
        private set

    constructor(type: EntityType<out SmokeDecoyEntity>, level: Level) : super(type, level)

    constructor(type: EntityType<out SmokeDecoyEntity>, level: Level, release: Boolean) : super(type, level) {
        releaseSmoke = release
    }

    constructor(level: Level) : super(ModEntities.SMOKE_DECOY.get(), level)

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> {
        return NetworkHooks.getEntitySpawningPacket(this)
    }

    override fun readAdditionalSaveData(compoundTag: CompoundTag) {
        if (compoundTag.contains("IgniteTime")) {
            this.igniteTime = compoundTag.getInt("IgniteTime")
        }
        if (compoundTag.contains("Life")) {
            this.life = compoundTag.getInt("Life")
        }
        if (compoundTag.contains("ReleaseSmoke")) {
            this.releaseSmoke = compoundTag.getBoolean("ReleaseSmoke")
        }
        if (compoundTag.contains("RColor")) {
            this.red = compoundTag.getFloat("RColor")
        }
        if (compoundTag.contains("GColor")) {
            this.green = compoundTag.getFloat("GColor")
        }
        if (compoundTag.contains("BColor")) {
            this.blue = compoundTag.getFloat("BColor")
        }
    }

    override fun addAdditionalSaveData(compoundTag: CompoundTag) {
        compoundTag.putInt("IgniteTime", igniteTime)
        compoundTag.putInt("Life", life)
        compoundTag.putBoolean("Release", this.releaseSmoke)
        compoundTag.putFloat("RColor", this.red)
        compoundTag.putFloat("GColor", this.green)
        compoundTag.putFloat("BColor", this.blue)
    }

    fun setColor(r: Float, g: Float, b: Float): SmokeDecoyEntity {
        this.red = r
        this.green = g
        this.blue = b
        return this
    }

    override fun defineSynchedData() {
    }

    override fun tick() {
        super.tick()
        this.move(MoverType.SELF, this.deltaMovement)
        if (tickCount == this.igniteTime) {
            if (releaseSmoke) {
                val level = this.level()
                if (level is ServerLevel) {
                    ParticleTool.sendParticle(
                        level, CustomSmokeOption(this.red, this.green, this.blue), this.xo, this.yo, this.zo,
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
