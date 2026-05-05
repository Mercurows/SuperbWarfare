package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.particle.CustomSmokeOption
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.min

open class M18SmokeGrenadeEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    private var count = 8
    private var fuse = 100
    var red: Float = 1.0f
        private set
    var green: Float = 1.0f
        private set
    var blue: Float = 1.0f
        private set

    constructor(type: EntityType<out M18SmokeGrenadeEntity>, level: Level) : super(type, level) {
        this.noCulling = true
    }

    constructor(type: EntityType<out M18SmokeGrenadeEntity>, x: Double, y: Double, z: Double, world: Level) : super(
        type,
        x,
        y,
        z,
        world
    ) {
        this.noCulling = true
    }

    constructor(entity: LivingEntity?, level: Level, fuse: Int) : super(
        ModEntities.M18_SMOKE_GRENADE.get(),
        entity,
        level
    ) {
        this.noCulling = true
        this.fuse = fuse
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putFloat("Fuse", this.fuse.toFloat())
        compound.putInt("Count", this.count)
        compound.putFloat("RColor", this.red)
        compound.putFloat("GColor", this.green)
        compound.putFloat("BColor", this.blue)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("Fuse")) {
            this.fuse = compound.getInt("Fuse")
        }
        if (compound.contains("Count")) {
            this.count = Mth.clamp(compound.getInt("Count"), 1, 64)
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

    override fun getDefaultItem(): Item {
        return ModItems.M18_SMOKE_GRENADE.get()
    }

    override fun onHit(result: HitResult) {
        when (result.type) {
            HitResult.Type.MISS -> {}
            HitResult.Type.BLOCK -> {
                val blockResult = result as BlockHitResult
                val resultPos = blockResult.blockPos
                val state = this.level().getBlockState(resultPos)
                val block = state.block
                val event = block.getSoundType(state, this.level(), resultPos, this).breakSound
                val speed = this.deltaMovement.length()
                if (speed > 0.1) {
                    val volume = min(4f, speed.toFloat() / 4f + 0.5f)
                    this.level().playSound(
                        null,
                        result.getLocation().x,
                        result.getLocation().y,
                        result.getLocation().z,
                        event,
                        SoundSource.AMBIENT,
                        volume,
                        1f
                    )
                }
                this.bounce(blockResult.direction)

                if (block is BellBlock) {
                    block.attemptToRing(this.level(), resultPos, blockResult.direction)
                }
            }

            HitResult.Type.ENTITY -> {
                val entityResult = result as EntityHitResult
                val entity = entityResult.entity
                val owner = this.owner
                if (entity == owner || entity == this.vehicle) return
                val speedE = this.deltaMovement.length()
                if (speedE > 0.1) {
                    if (owner is LivingEntity) {
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
                    entity.hurt(entity.damageSources().thrown(this, owner), 1f)
                }
                this.bounce(
                    Direction.getNearest(
                        this.deltaMovement.x(),
                        this.deltaMovement.y(),
                        this.deltaMovement.z()
                    ).opposite
                )
                this.deltaMovement = this.deltaMovement.multiply(0.25, 1.0, 0.25)
            }

            else -> {}
        }
    }

    private fun bounce(direction: Direction) {
        when (direction.axis) {
            Direction.Axis.X -> this.deltaMovement = this.deltaMovement.multiply(-0.5, 0.75, 0.75)
            Direction.Axis.Y -> {
                this.deltaMovement = this.deltaMovement.multiply(0.75, -0.25, 0.75)
                if (this.deltaMovement.y() < this.getCustomGravity()) {
                    this.deltaMovement = this.deltaMovement.multiply(1.0, 0.0, 1.0)
                }
            }

            Direction.Axis.Z -> this.deltaMovement = this.deltaMovement.multiply(0.75, 0.75, -0.5)
        }
    }

    override fun tick() {
        super.tick()
        --this.fuse

        if (tickCount > 200) {
            this.discard()
        }

        if (fuse == -20) {
            releaseSmoke()
        }

        val level = this.level()
        if (fuse == 0) {
            level.playSound(null, this, ModSounds.SM0KE_GRENADE_RELEASE.get(), this.soundSource, 2f, 1f)
        }

        if (fuse <= 0 && tickCount % 2 == 0) {
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

        if (isInFluidType) {
            deltaMovement = deltaMovement.scale(0.75)
        }

        if (level is ServerLevel) {
            ParticleTool.sendParticle(
                level, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
                1, 0.0, 0.0, 0.0, 0.01, true
            )
        }
    }

    fun releaseSmoke() {
        val vec3 = Vec3(1.0, 0.05, 0.0)

        for (i in 0..<this.count) {
            val decoy = SmokeDecoyEntity(ModEntities.SMOKE_DECOY.get(), this.level(), false)
            decoy.setPos(this.x, this.y + bbHeight, this.z)
            decoy.decoyShoot(this, vec3.yRot(i * (360f / this.count) * Mth.DEG_TO_RAD), 1.5f, 5f)
            this.level().addFreshEntity(decoy)
        }
    }

    override fun getDefaultGravity() = 0.07

    fun setColor(r: Float, g: Float, b: Float): M18SmokeGrenadeEntity {
        this.red = r
        this.green = g
        this.blue = b
        return this
    }

    override fun isFastMoving(): Boolean {
        return false
    }

    override fun getModel() = BedrockModelLoader.M18_SMOKE_GRENADE_MODEL
}
