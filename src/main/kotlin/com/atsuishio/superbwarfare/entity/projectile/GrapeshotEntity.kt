package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.min

open class GrapeshotEntity : FastThrowableProjectile {
    constructor(type: EntityType<out GrapeshotEntity>, level: Level) : super(type, level) {
        this.noCulling = true
    }

    constructor(entity: Entity?, level: Level, damage: Float) : super(ModEntities.GRAPESHOT.get(), entity, level) {
        this.noCulling = true
        this.damageValue = damage
    }

    override fun getDefaultItem(): Item {
        return ModItems.LARGE_SHELL_GS.get()
    }

    override fun performDamage(
        entity: Entity,
        damage: Float,
        isHeadshot: Boolean
    ) {
        entity.invulnerableTime = 0

        val headShotModifier = if (isHeadshot) this.getHeadShot() else 1f
        if (damage > 0) {
            entity.forceHurt(
                ModDamageTypes.causeGrapeShotHitDamage(this.level().registryAccess(), this, this.owner),
                damage * headShotModifier
            )
            entity.invulnerableTime = 0
        }
    }

    override fun afterHitBlock(result: BlockHitResult) {
        val level = this.level() as? ServerLevel ?: return

        val resultPos = result.blockPos
        val state = level.getBlockState(resultPos)

        val event = state.block.getSoundType(state, level, resultPos, this).breakSound
        val volume = min(4f, deltaMovement.length().toFloat() / 4f + 0.5f)
        val location = result.location

        level.playSound(
            null,
            location.x,
            location.y,
            location.z,
            event,
            SoundSource.AMBIENT,
            volume,
            1f
        )

        val face = result.direction
        val vx = face.stepX.toDouble()
        val vy = face.stepY.toDouble()
        val vz = face.stepZ.toDouble()
        val dir = Vec3(vx, vy, vz)
        summonVectorParticle(level, state, location, dir)
        level.playSound(
            null,
            BlockPos(location.x.toInt(), location.y.toInt(), location.z.toInt()),
            ModSounds.LAND.get(),
            SoundSource.BLOCKS,
            1f,
            1f
        )

        super.afterHitBlock(result)
    }

    fun summonVectorParticle(serverLevel: ServerLevel, state: BlockState, pos: Vec3, dir: Vec3) {
        val particleData = BlockParticleOption(ParticleTypes.BLOCK, state)
        for (i in 0..6) {
            val vec3 = randomVec(dir, 40.0)
            ParticleTool.sendParticle(
                serverLevel,
                particleData,
                pos.x + 0.05 * i * dir.x,
                pos.y + 0.05 * i * dir.y,
                pos.z + 0.05 * i * dir.z,
                0,
                vec3.x,
                vec3.y,
                vec3.z,
                10.0,
                true
            )
        }
        repeat(2) {
            val vec3 = randomVec(dir, 20.0)
            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.SMOKE,
                pos.x,
                pos.y,
                pos.z,
                0,
                vec3.x,
                vec3.y,
                vec3.z,
                0.05,
                true
            )
        }
        val soundType = state.soundType
        if (soundType === SoundType.METAL || soundType === SoundType.ANVIL || soundType === SoundType.CHAIN || soundType === SoundType.COPPER || soundType === SoundType.NETHERITE_BLOCK) {
            serverLevel.playSound(null, pos.x, pos.y, pos.z, ModSounds.HIT.get(), SoundSource.BLOCKS, 2f, 1f)
            repeat(2) {
                val vec3 = randomVec(dir, 80.0)
                ParticleTool.sendParticle(
                    serverLevel,
                    ModParticleTypes.FIRE_STAR.get(),
                    pos.x,
                    pos.y,
                    pos.z,
                    0,
                    vec3.x,
                    vec3.y,
                    vec3.z,
                    0.2 + 0.1 * Math.random(),
                    true
                )
            }
        }
    }

    override fun tick() {
        super.tick()

        if (!this.level().isClientSide()) {
            val startVec = this.position()
            val endVec = startVec.add(this.deltaMovement)
            val fluidResult = IAdvancedHitDetection.rayTraceBlocks(
                this.level(),
                ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this)
            ) { false }
            this.onHitWater(fluidResult.getLocation(), fluidResult)
        }

        this.deltaMovement = this.deltaMovement.multiply(0.96, 0.96, 0.96)
    }

    protected fun onHitWater(location: Vec3, result: BlockHitResult) {
        val level = this.level()
        if (level is ServerLevel) {
            val pos = result.blockPos
            val face = result.direction
            val state = level.getBlockState(pos)

            val vx = face.stepX.toDouble()
            val vy = face.stepY.toDouble()
            val vz = face.stepZ.toDouble()
            val dir = Vec3(vx, vy, vz).add(deltaMovement.normalize().scale(-0.1))

            if (state.block === Blocks.WATER) {
                if (!isInWater) {
                    val particleData = CustomCloudOption(1f, 1f, 1f, 80, 0.5f, 1f, cooldown = false, light = false)
                    for (i in 0..9) {
                        val vec3 = randomVec(dir, 40.0)
                        ParticleTool.sendParticle(
                            level,
                            particleData,
                            location.x + 0.12 * i * dir.x,
                            location.y + 0.12 * i * dir.y,
                            location.z + 0.12 * i * dir.z,
                            0,
                            vec3.x,
                            vec3.y,
                            vec3.z,
                            15.0,
                            true
                        )
                    }

                    ParticleTool.spawnBulletHitWaterParticles(level, location)
                    level.playSound(
                        null,
                        BlockPos(location.x.toInt(), location.y.toInt(), location.z.toInt()),
                        ModSounds.HIT_WATER.get(),
                        SoundSource.BLOCKS,
                        1f,
                        1f
                    )
                    this.discard()
                }
            } else if (state.block === Blocks.LAVA) {
                if (!isInLava) {
                    val particleData = BlockParticleOption(ParticleTypes.BLOCK, state)
                    for (i in 0..6) {
                        val vec3 = randomVec(dir, 20.0)
                        ParticleTool.sendParticle(
                            level,
                            particleData,
                            location.x + 0.1 * i * dir.x,
                            location.y + 0.1 * i * dir.y,
                            location.z + 0.1 * i * dir.z,
                            0,
                            vec3.x,
                            vec3.y,
                            vec3.z,
                            10.0,
                            true
                        )
                    }
                    ParticleTool.sendParticle(
                        level, ParticleTypes.LAVA, location.x, location.y, location.z,
                        4, 0.0, 0.0, 0.0, 0.6, true
                    )
                    level.playSound(
                        null,
                        BlockPos(location.x.toInt(), location.y.toInt(), location.z.toInt()),
                        SoundEvents.LAVA_POP,
                        SoundSource.BLOCKS,
                        1f,
                        1f
                    )
                    this.discard()
                }
            }
        }
    }

    fun randomVec(vec3: Vec3, spread: Double): Vec3 {
        return vec3.normalize().add(
            this.random.triangle(0.0, 0.0172275 * spread),
            this.random.triangle(0.0, 0.0172275 * spread),
            this.random.triangle(0.0, 0.0172275 * spread)
        )
    }

    override fun isFastMoving(): Boolean {
        return false
    }
}
