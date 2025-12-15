package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitBlock
import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitEntity
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage
import com.atsuishio.superbwarfare.tools.*
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn
import java.util.function.Consumer

abstract class FastThrowableProjectile : ThrowableItemProjectile, CustomSyncMotionEntity, IEntityWithComplexSpawn,
    ExplosiveProjectile {

    @JvmField
    var damage: Float = 0f

    @JvmField
    var explosionDamage: Float = 0f

    @JvmField
    var explosionRadius: Float = 0f

    @JvmField
    var durability: Int = 50

    @JvmField
    var firstHit: Boolean = true

    @JvmField
    var gravity: Float = 0.05f

    private var isFastMoving = false
    private val currentChunks = mutableSetOf<ChunkPos>()
    private var lastChunkPos: ChunkPos? = null

    constructor(pEntityType: EntityType<out ThrowableItemProjectile>, pLevel: Level) : super(pEntityType, pLevel)

    constructor(
        pEntityType: EntityType<out ThrowableItemProjectile>,
        pX: Double,
        pY: Double,
        pZ: Double,
        pLevel: Level
    ) : super(pEntityType, pX, pY, pZ, pLevel)

    constructor(pEntityType: EntityType<out ThrowableItemProjectile>, shooter: Entity?, pLevel: Level) : super(
        pEntityType,
        pLevel
    ) {
        this.owner = shooter
        if (shooter != null) {
            this.setPos(shooter.x, shooter.eyeY - 0.1, shooter.z)
        }
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)

        if (compound.contains("Damage")) {
            this.damage = compound.getFloat("Damage")
        }
        if (compound.contains("ExplosionDamage")) {
            this.explosionDamage = compound.getFloat("ExplosionDamage")
        }
        if (compound.contains("Radius")) {
            this.explosionRadius = compound.getFloat("Radius")
        }
        if (compound.contains("Durability")) {
            this.durability = compound.getInt("Durability")
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)

        if (this.damage > 0) {
            compound.putFloat("Damage", this.damage)
        }
        if (this.explosionDamage > 0) {
            compound.putFloat("ExplosionDamage", this.explosionDamage)
        }
        if (this.explosionRadius > 0) {
            compound.putFloat("Radius", this.explosionRadius)
        }
        if (this.durability > 0) {
            compound.putInt("Durability", this.durability)
        }
    }

    override fun tick() {
        super.tick()

        if (!this.isFastMoving && this.isFastMoving() && this.level().isClientSide) {
            playFlySound.accept(this)
            playNearFlySound.accept(this)
        }
        this.isFastMoving = this.isFastMoving()

        var vec3 = this.deltaMovement
        val friction = if (this.isInWater) {
            0.8f
        } else {
            0.99f
        }
        // 撤销重力影响
        vec3 = vec3.add(0.0, this.getGravity(), 0.0)
        // 重新计算动量
        this.deltaMovement = vec3.scale((1 / friction).toDouble())
        // 重新应用重力
        this.applyGravity()

        // 同步动量
        this.syncMotion()

        // 更新区块加载位置
        val level = level()
        if (level is ServerLevel && forceLoadChunk()) {
            updateChunkLoading(level)
        }
    }

    override fun onHitEntity(pResult: EntityHitResult) {
        super.onHitEntity(pResult)

        postEvent(
            HitEntity(
                this.owner,
                this,
                pResult.entity,
                pResult.getLocation()
            )
        )
    }

    override fun onHitBlock(pResult: BlockHitResult) {
        super.onHitBlock(pResult)

        postEvent(
            HitBlock(
                pResult.blockPos,
                this.level().getBlockState(pResult.blockPos),
                pResult.direction,
                this.owner,
                this,
                pResult.getLocation()
            )
        )
    }

    fun destroyBlock() {
        if (!ExplosionConfig.EXPLOSION_DESTROY.get()) return

        val posO = Vec3(xo, yo, zo)
        val blockList = TraceTool.getBlocksAlongRay(posO, deltaMovement, deltaMovement.length())

        for (pos in blockList) {
            val blockState = level().getBlockState(pos)

            if (!blockState.`is`(Blocks.AIR)) {
                val hardness = this.level().getBlockState(pos).block.defaultDestroyTime()

                val resistance = 1 - Mth.clamp((hardness / 100).toDouble(), 0.0, 0.8)
                deltaMovement = deltaMovement.multiply(resistance, resistance, resistance)

                if (blockState.canOcclude()) {
                    durability -= 10 + (0.5 * hardness).toInt()
                }

                if (hardness <= durability && hardness != -1f && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                    this.level().destroyBlock(pos, true)
                }
                if (hardness == -1f || hardness > durability || durability <= 0) {
                    causeExplode(pos.center)
                    discard()
                    break
                }
            }
        }
    }

    open fun buildExplosion(vec3: Vec3): CustomExplosion.Builder {
        return CustomExplosion.Builder(this)
            .attacker(this.owner)
            .damage(explosionDamage)
            .radius(explosionRadius)
            .position(vec3)
            .withParticleType(explosionParticleType(explosionRadius))
    }

    fun causeExplode(vec3: Vec3) {
        buildExplosion(vec3).explode()

        if (discardAfterExplode()) {
            this.discard()
        }
    }

    fun explosionParticleType(radius: Float) = if (radius <= 2) {
        ParticleTool.ParticleType.MINI
    } else if (radius <= 4) {
        ParticleTool.ParticleType.SMALL
    } else if (radius < 7) {
        ParticleTool.ParticleType.MEDIUM
    } else if (radius < 10) {
        ParticleTool.ParticleType.LARGE
    } else if (radius < 20) {
        ParticleTool.ParticleType.HUGE
    } else {
        ParticleTool.ParticleType.GIANT
    }

    open fun discardAfterExplode() = false

    private fun updateChunkLoading(serverLevel: ServerLevel) {
        val currentPos = ChunkPos(blockPosition())

        // 检查是否需要更新
        if (lastChunkPos == currentPos) return

        // 计算需要加载的新区块
        val neededChunks = mutableSetOf<ChunkPos>()
        for (x in -CHUNK_RADIUS..CHUNK_RADIUS) {
            for (z in -CHUNK_RADIUS..CHUNK_RADIUS) {
                neededChunks += ChunkPos(currentPos.x + x, currentPos.z + z)
            }
        }

        // 释放不再需要的区块
        for (pos in currentChunks - neededChunks) {
            ChunkLoadManager.releaseChunk(serverLevel, pos)
            currentChunks.remove(pos)
        }

        // 加载新区块
        for (pos in neededChunks) {
            if (pos !in currentChunks) {
                ChunkLoadManager.forceChunk(serverLevel, pos)
                currentChunks += pos
            }
        }

        lastChunkPos = currentPos
    }

    override fun remove(reason: RemovalReason) {
        val level = level()
        if (level is ServerLevel) {
            // 释放所有加载的区块
            for (pos in currentChunks) {
                ChunkLoadManager.releaseChunk(level, pos)
            }
            currentChunks.clear()
        }
        super.remove(reason)
    }

    override fun syncMotion() {
        if (this.level().isClientSide) return
        if (!shouldSyncMotion()) return

        if (this.tickCount % this.type.updateInterval() == 0) {
            sendPacketToTracking(ClientMotionSyncMessage(this))
        }
    }

    open fun isFastMoving() = this.deltaMovement.length() >= 0.5

    open fun shouldSyncMotion() = false

    override fun writeSpawnData(buffer: RegistryFriendlyByteBuf) {
        val motion = this.deltaMovement
        buffer.writeFloat(motion.x.toFloat())
        buffer.writeFloat(motion.y.toFloat())
        buffer.writeFloat(motion.z.toFloat())
    }

    override fun readSpawnData(additionalData: RegistryFriendlyByteBuf) {
        this.setDeltaMovement(
            additionalData.readFloat().toDouble(),
            additionalData.readFloat().toDouble(),
            additionalData.readFloat().toDouble()
        )
    }

    open val sound: SoundEvent = SoundEvents.EMPTY

    open val volume = 0.5f

    open fun forceLoadChunk() = false

    override fun shouldRenderAtSqrDistance(pDistance: Double) = true

    override fun setDamage(damage: Float) {
        this.damage = damage
    }

    override fun setExplosionDamage(explosionDamage: Float) {
        this.explosionDamage = explosionDamage
    }

    override fun setExplosionRadius(radius: Float) {
        this.explosionRadius = radius
    }

    public override fun getDefaultGravity() = this.gravity.toDouble()

    override fun setGravity(gravity: Float) {
        this.gravity = gravity
    }

    open fun largeTrail() {
        if (level().isClientSide && tickCount > 2) {
            val l = deltaMovement.length()
            var i = 0.0
            while (i < l) {
                val startPos = Vec3(xo, yo, zo)
                val pos = startPos.add(deltaMovement.normalize().scale(-i))
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0)
                i += 2.0
            }
        }
    }

    fun mediumTrail() {
        if (level().isClientSide && tickCount > 2) {
            val l = deltaMovement.length()
            var i = 0.0
            while (i < l) {
                val startPos = Vec3(xo, yo, zo)
                val pos = startPos.add(deltaMovement.normalize().scale(-i))
                val random = this.random.nextFloat()
                level().addParticle(
                    CustomCloudOption(
                        0.6f,
                        0.58f,
                        0.57f,
                        (120 + 40 * random).toInt(),
                        1.5f + 0.5f * random,
                        0f,
                        false,
                        false
                    ), pos.x + 0.25f * random, pos.y + 0.25f * random, pos.z + 0.25f * random, 0.0, 0.0, 0.0
                )
                i += 2.0
            }
        }
    }

    fun smallTrail() {
        if (level().isClientSide && tickCount > 2) {
            val l = deltaMovement.length()
            var i = 0.0
            while (i < l) {
                val startPos = Vec3(xo, yo, zo)
                val pos = startPos.add(deltaMovement.normalize().scale(-i))
                val random = this.random.nextFloat()
                level().addAlwaysVisibleParticle(
                    ParticleTypes.SMOKE,
                    true,
                    pos.x + 0.25f * random,
                    pos.y + 0.25f * random,
                    pos.z + 0.25f * random,
                    0.0,
                    0.0,
                    0.0
                )
                i += 2.0
            }
        }
    }

    companion object {
        @JvmField
        var playFlySound: Consumer<FastThrowableProjectile> = Consumer { }

        @JvmField
        var playNearFlySound: Consumer<FastThrowableProjectile> = Consumer { }

        private const val CHUNK_RADIUS = 1 // 3x3区块
    }
}
