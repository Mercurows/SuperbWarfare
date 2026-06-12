package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitBlock
import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitEntity
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.client.particle.CustomFlareOption
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.postEvent
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
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn
import net.neoforged.neoforge.network.PacketDistributor
import java.util.function.Consumer

abstract class FastThrowableProjectile : ThrowableItemProjectile, IFastMotionSync, IEntityWithComplexSpawn,
    IBulletProperties {
    var damageValue: Float = 0f
    var explosionDamageValue: Float = 0f
    var explosionRadiusValue: Float = 0f
    var gravityValue: Float = 0.05f
    var lifeValue: Int = 400
    var durability: Int = 50
    var firstHit: Boolean = true

    override var damage: Float
        get() = damageValue
        set(v) {
            damageValue = v
        }
    override var explosionDamage: Float
        get() = explosionDamageValue
        set(v) {
            explosionDamageValue = v
        }
    override var explosionRadius: Float
        get() = explosionRadiusValue
        set(v) {
            explosionRadiusValue = v
        }
    override var life: Int
        get() = lifeValue
        set(v) {
            lifeValue = v
        }

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
        this.baseTick()
        this.updateRotation()

        if (!this.isFastMoving && this.isFastMoving() && this.level().isClientSide) {
            playFlySound.accept(this)
            playNearFlySound.accept(this)
        }
        this.isFastMoving = this.isFastMoving()

        if (!this.level().isClientSide()) {
            val startVec = this.position()
            var endVec = startVec.add(this.deltaMovement)

            // Block collision
            val blockHit = this.level().clip(
                ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)
            )
            if (blockHit.type != HitResult.Type.MISS) {
                endVec = blockHit.location
            }

            // OBB-based entity collision (replaces vanilla ProjectileUtil path)
            val entities = this.level().getEntities(
                this,
                this.boundingBox.expandTowards(this.deltaMovement).inflate(1.0),
                ProjectileEntity.PROJECTILE_TARGETS_FAST
            )
            var closestEntity: Entity? = null
            var closestHitVec: Vec3? = null
            var closestDistSqr = Double.MAX_VALUE

            for (entity in entities) {
                if (entity == this.owner || this.owner != null && entity == this.owner!!.vehicle) continue
                if (this.owner != null && entity.rootVehicle === this.owner!!.rootVehicle) continue

                // For OBB entities: use OBB clip only, never fall back to AABB
                val hitVec = if (entity is OBBEntity && !entity.enableAABB()) {
                    ProjectileEntity.clipObb(this, entity, startVec, endVec)
                } else {
                    // Non-OBB entities: use vanilla AABB clip
                    entity.boundingBox.clip(startVec, endVec).orElse(null)
                }

                if (hitVec != null) {
                    val d = startVec.distanceToSqr(hitVec)
                    if (d < closestDistSqr) {
                        closestDistSqr = d
                        closestEntity = entity
                        closestHitVec = hitVec
                    }
                }
            }

            // Process block hit first, then entity hit
            if (blockHit.type != HitResult.Type.MISS) {
                this.onHitBlock(blockHit)
            }
            if (closestEntity != null) {
                val result = EntityHitResult(closestEntity, closestHitVec!!)
                this.onHitEntity(result)
            }

            // Movement
            this.setPos(this.x + deltaMovement.x, this.y + deltaMovement.y, this.z + deltaMovement.z)
        } else {
            this.setPosRaw(this.x + deltaMovement.x, this.y + deltaMovement.y, this.z + deltaMovement.z)
        }

        // Custom friction and gravity
        // Note: super.tick() is not called, so vanilla gravity was never applied.
        // We apply friction first, then our own gravity directly (no undo needed).
        val friction = if (this.isInWater) {
            0.8f
        } else {
            0.99f
        }

        this.deltaMovement = this.deltaMovement.scale((1 / friction).toDouble())
        this.setDeltaMovement(
            this.deltaMovement.x,
            this.deltaMovement.y - this.gravity,
            this.deltaMovement.z
        )

        // 同步动量
        this.syncMotion()

        // 更新区块加载位置
        if (level() is ServerLevel) {
            if (forceLoadChunk() && ProjectileConfig.PROJECTILE_CHUNK_LOADING.get()) {
                this.keepChunkLoaded(this.position())
                this.keepChunkLoaded(position().add(this.deltaMovement.normalize().scale(16.0)))
            }

            if (tickCount > life) {
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
        postEvent(
            HitEntity(
                this.owner,
                this,
                result.entity,
                result.location
            )
        )
    }

    override fun onHitBlock(result: BlockHitResult) {
        postEvent(
            HitBlock(
                result.blockPos,
                this.level().getBlockState(result.blockPos),
                result.direction,
                this.owner,
                this,
                result.location
            )
        )
    }

    open fun destroyBlock(blockHitResult: BlockHitResult) {
        val resultPos = blockHitResult.blockPos
        val hardness = this.level().getBlockState(resultPos).block.defaultDestroyTime()
        if (hardness != -1f) {
            if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
                if (firstHit) {
                    causeExplode(blockHitResult.location)
                    firstHit = false
                    queueServerWork(3) { this.discard() }
                }
                if (ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                    this.level().destroyBlock(resultPos, true)
                }
            }
        } else {
            causeExplode(blockHitResult.location)
            this.discard()
        }
        if (!ExplosionConfig.EXPLOSION_DESTROY.get()) {
            causeExplode(blockHitResult.location)
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
        } else if (radius in 20.0..<30.0) {
            ParticleTool.ParticleType.GIANT
        } else {
            ParticleTool.ParticleType.EPIC
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

    override fun isFastMoving(): Boolean {
        return this.deltaMovement.length() >= 0.5
    }

    override fun shouldSyncMotion(): Boolean {
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

    override fun setCustomGravity(gravity: Float) {
        this.gravityValue = gravity
    }

    open fun getCustomGravity(): Float {
        return this.gravityValue
    }

    open fun hugeMissileTrail() {
        if (level().isClientSide) {
            val l = deltaMovement.length()
            var i = 0.0
            while (i < l) {
                val startPos = Vec3(xo, yo + bbHeight / 2, zo)
                val pos = startPos.add(deltaMovement.normalize().scale(-i))
                val random = 2 * (this.random.nextFloat() - 0.5f)
                level().addParticle(
                    CustomFlareOption(
                        0.5f,
                        0.43f,
                        0.36f,
                        700,
                        0.985f,
                        (10 + 8 * random).toInt(),
                        0.03f
                    ), pos.x + random * 0.25, pos.y + random * 0.25, pos.z + random * 0.25, 0.0, 0.0, 0.0
                )
                i += 2.0
            }
        }
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