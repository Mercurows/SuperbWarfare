package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitBlock
import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitEntity
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.TicketType
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn
import net.neoforged.neoforge.network.PacketDistributor
import java.util.function.Consumer

abstract class FastThrowableProjectile : ThrowableItemProjectile, CustomSyncMotionEntity, IEntityWithComplexSpawn,
    ExplosiveProjectile {
    var damageValue: Float = 0f
    var explosionDamageValue: Float = 0f
    var explosionRadiusValue: Float = 0f
    var gravityValue: Float = 0.05f
    var lifeValue: Int = 400
    var durability: Int = 50
    var firstHit: Boolean = true

    private var isFastMoving = false

    var exploded: Boolean = false

    constructor(pEntityType: EntityType<out ThrowableItemProjectile>, pLevel: Level) : super(pEntityType, pLevel)

    constructor(
        pEntityType: EntityType<out ThrowableItemProjectile>,
        pX: Double,
        pY: Double,
        pZ: Double,
        pLevel: Level
    ) : super(pEntityType, pX, pY, pZ, pLevel)

    constructor(pEntityType: EntityType<out ThrowableItemProjectile>, pShooter: Entity?, pLevel: Level) : super(
        pEntityType,
        pLevel
    ) {
        this.owner = pShooter
        if (pShooter != null) {
            this.setPos(pShooter.x, pShooter.eyeY - 0.1, pShooter.z)
        }
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("Damage")) {
            this.damageValue = compound.getFloat("Damage")
        }
        if (compound.contains("ExplosionDamage")) {
            this.explosionDamageValue = compound.getFloat("ExplosionDamage")
        }
        if (compound.contains("Radius")) {
            this.explosionRadiusValue = compound.getFloat("Radius")
        }
        if (compound.contains("Durability")) {
            this.durability = compound.getInt("Durability")
        }
        if (compound.contains("Life")) {
            this.lifeValue = compound.getInt("Life")
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)

        if (this.damageValue > 0) {
            compound.putFloat("Damage", this.damageValue)
        }
        if (this.explosionDamageValue > 0) {
            compound.putFloat("ExplosionDamage", this.explosionDamageValue)
        }
        if (this.explosionRadiusValue > 0) {
            compound.putFloat("Radius", this.explosionRadiusValue)
        }
        if (this.durability > 0) {
            compound.putInt("Durability", this.durability)
        }
        if (this.lifeValue > 0) {
            compound.putInt("Life", this.lifeValue)
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
        vec3 = vec3.add(0.0, this.gravityValue.toDouble(), 0.0)
        // 重新计算动量
        this.deltaMovement = vec3.scale((1 / friction).toDouble())

        // 重新应用重力
        val vec31 = this.deltaMovement
        this.setDeltaMovement(vec31.x, vec31.y - this.gravityValue.toDouble(), vec31.z)

        // 同步动量
        this.syncMotion()

        // 更新区块加载位置
        if (level() is ServerLevel) {
            if (forceLoadChunk() && ProjectileConfig.PROJECTILE_CHUNK_LOADING.get()) {
                this.keepChunkLoaded(this.position())
                this.keepChunkLoaded(position().add(this.deltaMovement.normalize().scale(16.0)))
            }

            if (tickCount > getLife()) {
                if (explosionRadiusValue > 0) {
                    causeExplode(position())
                }
                this.discard()
            }
        }
    }

    override fun updateRotation() {
        val vec3 = this.deltaMovement
        val d0 = vec3.horizontalDistance()
        this.xRot = lerpRotation(
            this.xRotO,
            -(Mth.atan2(vec3.y, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        )
        this.yRot = lerpRotation(
            this.yRotO,
            -(Mth.atan2(vec3.x, vec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        )
    }

    override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        NeoForge.EVENT_BUS.post(
            HitEntity(
                this.owner,
                this,
                result.entity,
                result.getLocation()
            )
        )
    }

    override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)
        NeoForge.EVENT_BUS.post(
            HitBlock(
                result.blockPos,
                this.level().getBlockState(result.blockPos),
                result.direction,
                this.owner,
                this,
                result.getLocation()
            )
        )
    }

    open fun destroyBlock(blockHitResult: BlockHitResult) {
        val resultPos = blockHitResult.blockPos
        val hardness = this.level().getBlockState(resultPos).block.defaultDestroyTime()
        if (hardness != -1f) {
            if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
                if (firstHit) {
                    causeExplode(blockHitResult.getLocation())
                    firstHit = false
                    queueServerWork(3) { this.discard() }
                }
                if (ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                    this.level().destroyBlock(resultPos, true)
                }
            }
        } else {
            causeExplode(blockHitResult.getLocation())
            this.discard()
        }
        if (!ExplosionConfig.EXPLOSION_DESTROY.get()) {
            causeExplode(blockHitResult.getLocation())
            this.discard()
        }
    }

    open fun buildExplosion(vec3: Vec3): CustomExplosion.Builder {
        return CustomExplosion.Builder(this)
            .attacker(this.owner)
            .damage(explosionDamageValue)
            .radius(explosionRadiusValue)
            .position(vec3)
            .withParticleType(explosionParticleType(explosionRadiusValue))
    }

    open fun causeExplode(vec3: Vec3) {
        if (!exploded) {
            exploded = true
            buildExplosion(vec3).explode()
        }

        if (discardAfterExplode()) {
            this.discard()
        }
    }

    open fun explosionParticleType(radius: Float): ParticleTool.ParticleType {
        return if (radius < 2.0) {
            ParticleTool.ParticleType.MINI
        } else if (radius in 2.0..<4.0) {
            ParticleTool.ParticleType.SMALL
        } else if (radius in 4.0..<7.0) {
            ParticleTool.ParticleType.MEDIUM
        } else if (radius in 7.0..<10.0) {
            ParticleTool.ParticleType.LARGE
        } else if (radius in 10.0..<20.0) {
            ParticleTool.ParticleType.HUGE
        } else {
            ParticleTool.ParticleType.GIANT
        }
    }

    open fun discardAfterExplode(): Boolean {
        return false
    }

    open fun keepChunkLoaded(position: Vec3) {
        val chunkPos = ChunkPos(BlockPos.containing(position))
        (level() as ServerLevel).chunkSource.addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 3, this.id)
    }

    override fun syncMotion() {
        if (this.level().isClientSide) return
        if (!shouldSyncMotion()) return

        if (this.tickCount % this.type.updateInterval() == 0) {
            PacketDistributor.sendToPlayersTrackingEntity(this, ClientMotionSyncMessage(this))
        }
    }

    open fun isFastMoving(): Boolean {
        return this.deltaMovement.length() >= 0.5
    }

    open fun shouldSyncMotion(): Boolean {
        return true
    }

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

    open fun getSound(): SoundEvent = SoundEvents.EMPTY

    open fun getVolume(): Float = 0.5f

    open fun forceLoadChunk(): Boolean {
        return false
    }

    override fun shouldRenderAtSqrDistance(pDistance: Double): Boolean {
        return true
    }

    override fun setDamage(damage: Float) {
        this.damageValue = damage
    }

    override fun setExplosionDamage(explosionDamage: Float) {
        this.explosionDamageValue = explosionDamage
    }

    override fun setExplosionRadius(radius: Float) {
        this.explosionRadiusValue = radius
    }

    override fun setLife(life: Int) {
        this.lifeValue = life
    }

    open fun getLife(): Int {
        return lifeValue
    }

    open fun getCustomGravity(): Float {
        return this.gravityValue
    }

    override fun setGravity(gravity: Float) {
        this.gravityValue = gravity
    }

    open fun largeTrail() {
        if (level().isClientSide && tickCount > 2) {
            val l = deltaMovement.length()
            var i = 0.0
            while (i < l) {
                val startPos = Vec3(xo, yo + bbHeight / 2, zo)
                val pos = startPos.add(deltaMovement.normalize().scale(-i))
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0)
                i += 2.0
            }
        }
    }

    open fun mediumTrail() {
        if (level().isClientSide && tickCount > 2) {
            val l = deltaMovement.length()
            var i = 0.0
            while (i < l) {
                val startPos = Vec3(xo, yo + bbHeight / 2, zo)
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
                        cooldown = false,
                        light = false
                    ), pos.x + 0.25f * random, pos.y + 0.25f * random, pos.z + 0.25f * random, 0.0, 0.0, 0.0
                )
                i += 2.0
            }
        }
    }

    open fun smallTrail() {
        if (level().isClientSide && tickCount > 2) {
            val l = deltaMovement.length()
            var i = 0.0
            while (i < l) {
                val startPos = Vec3(xo, yo + bbHeight / 2, zo)
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

    fun checkNoClip(target: Entity, pos: Vec3): Boolean {
        return this.level().clip(
            ClipContext(
                pos, target.boundingBox.center,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this
            )
        ).type != HitResult.Type.BLOCK
    }

    override fun shoot(pX: Double, pY: Double, pZ: Double, pVelocity: Float, pInaccuracy: Float) {
        val vec3 = (Vec3(pX, pY, pZ)).normalize().add(
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble())
        ).scale(pVelocity.toDouble())
        this.deltaMovement = vec3
        val d0 = vec3.horizontalDistance()
        this.yRot = (-Mth.atan2(vec3.x, vec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        this.xRot = (-Mth.atan2(vec3.y, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        this.yRotO = this.yRot
        this.xRotO = this.xRot
    }

    override fun getDefaultGravity(): Double {
        return this.gravityValue.toDouble()
    }

    companion object {
        var playFlySound: Consumer<FastThrowableProjectile> = Consumer { }
        var playNearFlySound: Consumer<FastThrowableProjectile> = Consumer { }
    }
}