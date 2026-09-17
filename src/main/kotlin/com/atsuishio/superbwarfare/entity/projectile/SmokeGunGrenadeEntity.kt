package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.particle.CustomSmokeOption
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.sendPacketTo
import com.atsuishio.superbwarfare.world.phys.ExtendedEntityRayTraceResult
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.min

open class SmokeGunGrenadeEntity(type: EntityType<out SmokeGunGrenadeEntity>, level: Level) : BounceProjectile(
    type,
    level
), BasicGeoProjectileEntity {
    private var count = 8
    private var release = false
    private var releaseTick = 0
    var red: Float = 1.0f
        private set
    var green: Float = 1.0f
        private set
    var blue: Float = 1.0f
        private set

    init {
        this.damageValue = 1f
        this.headShotValue = 5f
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putBoolean("Release", false)
        compound.putInt("ReleaseTick", this.releaseTick)
        compound.putInt("Count", this.count)
        compound.putFloat("RColor", this.red)
        compound.putFloat("GColor", this.green)
        compound.putFloat("BColor", this.blue)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("Release")) {
            this.release = compound.getBoolean("Release")
        }
        if (compound.contains("Count")) {
            this.count = Mth.clamp(compound.getInt("Count"), 1, 64)
        }
        if (compound.contains("ReleaseTick")) {
            this.releaseTick = compound.getInt("ReleaseTick")
        }
        if (compound.contains("RColor")) {
            this.red = compound.getFloat("RColor")
        }
        if (compound.contains("GColor")) {
            this.green = compound.getFloat("GColor")
        }
        if (compound.contains("BColor")) {
            this.blue = compound.getFloat("BColor")
        }
    }

    override fun canPassThroughFluid() = true

    override fun getDefaultItem(): Item {
        return ModItems.M18_SMOKE_GRENADE.get()
    }

    override fun afterHitEntity(result: EntityHitResult) {
        if (result !is ExtendedEntityRayTraceResult) return
        val entity = result.entity
        val owner = this.owner
        if (entity == owner || entity == this.vehicle) return
        val speedE = this.deltaMovement.length()
        if (speedE > 0.1) {
            if (owner is ServerPlayer) {
                owner.level().playSound(
                    null,
                    owner.blockPosition(),
                    ModSounds.INDICATION.get(),
                    SoundSource.VOICE,
                    1f,
                    1f
                )

                sendPacketTo(owner, ClientIndicatorMessage(0, 5))
            }
        }
        this.deltaMovement = Vec3.ZERO
        this.deltaMovement = this.deltaMovement.multiply(0.25, 1.0, 0.25)
        if (!release) {
            release = true
            releaseTick = tickCount
            val level = this.level()
            releaseSmoke()
            level.playSound(null, this, ModSounds.SM0KE_GRENADE_RELEASE.get(), this.soundSource, 2f, 1f)
        }
    }

    override fun afterHitBlock(result: BlockHitResult) {
        val resultPos = result.blockPos
        val state = this.level().getBlockState(resultPos)
        val block = state.block
        val event = block.getSoundType(state, this.level(), resultPos, this).breakSound
        val speed = this.deltaMovement.length()
        if (speed > 0.5) {
            val volume = min(4f, speed.toFloat() / 4f + 0.5f)
            this.level().playSound(
                null,
                result.location.x,
                result.location.y,
                result.location.z,
                event,
                SoundSource.AMBIENT,
                volume,
                1f
            )
        }
        if (!release) {
            release = true
            releaseTick = tickCount
            val level = this.level()
            releaseSmoke()
            level.playSound(null, this, ModSounds.SM0KE_GRENADE_RELEASE.get(), this.soundSource, 2f, 1f)
        }
        this.deltaMovement = Vec3.ZERO
    }

    override fun tick() {
        super.tick()

        if (tickCount > releaseTick + 100) {
            this.discard()
        }

        val level = this.level()

        if (release && tickCount % 2 == 0) {
            if (level is ServerLevel) {
                ParticleTool.sendParticle(
                    level,
                    CustomSmokeOption(this.red, this.green, this.blue),
                    this.x,
                    this.y + bbHeight,
                    this.z,
                    8,
                    0.075,
                    0.01,
                    0.075,
                    0.08,
                    true
                )
            }
        }

        if (level is ServerLevel) {
            ParticleTool.sendParticle(
                level, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
                1, 0.0, 0.0, 0.0, 0.01, true
            )
        }
    }

    open fun releaseSmoke() {
        val vec3 = Vec3(1.0, 0.05, 0.0)

        for (i in 0..<this.count) {
            val decoy = SmokeDecoyEntity(ModEntities.SMOKE_DECOY.get(), this.level(), false)
            decoy.setPos(this.x, this.y + bbHeight, this.z)
            decoy.decoyShoot(this, vec3.yRot(i * (360f / this.count) * Mth.DEG_TO_RAD), 1.5f, 5f)
            this.level().addFreshEntity(decoy)
        }
    }

    override fun getCustomGravity(): Float {
        return 0.07f
    }

    fun setColor(r: Float, g: Float, b: Float): SmokeGunGrenadeEntity {
        this.red = r
        this.green = g
        this.blue = b
        return this
    }

    override fun isFastMoving(): Boolean {
        return true
    }
}
