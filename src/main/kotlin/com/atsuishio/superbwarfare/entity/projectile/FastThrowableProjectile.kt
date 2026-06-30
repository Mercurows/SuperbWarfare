package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitBlock
import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitEntity
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.client.particle.CustomFlareOption
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.projectile.IAdvancedHitDetection.Companion.rayTraceBlocks
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.weapon.BeastItem.Companion.beastKill
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.network.message.receive.MissileTrailParticleMessage
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.world.chunk.ChunkLoadManager
import com.atsuishio.superbwarfare.world.phys.ExtendedEntityRayTraceResult
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.entity.IEntityAdditionalSpawnData
import net.minecraftforge.entity.PartEntity
import net.minecraftforge.network.NetworkHooks
import java.util.function.Consumer
import java.util.function.Predicate

abstract class FastThrowableProjectile : ThrowableItemProjectile, IFastMotionSync, IEntityAdditionalSpawnData,
    IBulletProperties, IAdvancedHitDetection {
    protected var damageValue: Float = 0f
    protected var explosionDamageValue: Float = 0f
    protected var explosionRadiusValue: Float = 0f
    protected var headShotValue = 1f
    protected var legShotValue = 1f
    protected var velocityValue = 4f
    protected var gravityValue: Float = 0.05f
    protected var lifeValue: Int = 400
    protected var durability: Int = 50
    protected var firstHit: Boolean = true
    protected var beastValue = false
    protected var penetratingValue: Boolean = false
    protected val effectsValue: MutableSet<MobEffectInstance> = hashSetOf()

    override fun getDamage(): Float = damageValue
    override fun setDamage(value: Float) {
        damageValue = value
    }

    override fun getExplosionDamage(): Float = explosionDamageValue
    override fun setExplosionDamage(value: Float) {
        explosionDamageValue = value
    }

    override fun getExplosionRadius(): Float = explosionRadiusValue
    override fun setExplosionRadius(value: Float) {
        explosionRadiusValue = value
    }

    override fun getLife(): Int = lifeValue
    override fun setLife(value: Int) {
        lifeValue = value
    }

    override fun getVelocity(): Float = velocityValue
    override fun setVelocity(value: Float) {
        velocityValue = value
    }

    override fun isBeast(): Boolean = beastValue
    override fun setBeast(value: Boolean) {
        beastValue = value
    }

    override fun isPenetrating(): Boolean = penetratingValue
    override fun setPenetrating(value: Boolean) {
        penetratingValue = value
    }

    override fun getHeadShot(): Float = headShotValue
    override fun setHeadShot(value: Float) {
        headShotValue = value
    }

    override fun getLegShot(): Float = legShotValue
    override fun setLegShot(value: Float) {
        legShotValue = value
    }

    override fun getEffects(): Set<MobEffectInstance> = effectsValue
    override fun setEffects(effects: List<MobEffectInstance>) {
        this.effectsValue.addAll(effects)
    }

    private var isFastMoving = false

    var exploded: Boolean = false

    constructor(entityType: EntityType<out ThrowableItemProjectile>, level: Level) : super(entityType, level)

    constructor(
        entityType: EntityType<out ThrowableItemProjectile>,
        x: Double,
        y: Double,
        z: Double,
        level: Level
    ) : super(entityType, level) {
        this.setPos(x, y, z)
    }

    constructor(entityType: EntityType<out ThrowableItemProjectile>, shooter: Entity?, level: Level) :
            super(entityType, level) {
        this.owner = shooter
        if (shooter != null) {
            this.setPos(shooter.x, shooter.eyeY - 0.1, shooter.z)
        }
    }

    init {
        this.noCulling = true
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

        if (compound.contains("SyncedTick")) {
            syncedTick = compound.getInt("SyncedTick")
        }

        this.effectsValue.addAll(PotionUtils.getCustomEffects(compound))
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

        compound.putInt("SyncedTick", syncedTick)

        if (!this.effectsValue.isEmpty()) {
            val list = ListTag()
            for (instance in this.effectsValue) {
                list.add(instance.save(CompoundTag()))
            }
            compound.put("CustomPotionEffects", list)
        }
    }

    override fun tick() {
        super.baseTick()
        if (!level().isClientSide) syncedTick++

        val level = this.level()
        if (!level.isClientSide() && this.tickCount > this.getNoHitTicks()) {
            val startVec = this.position()
            val fullEndVec = startVec.add(this.deltaMovement)

            // 1. 查找最近的方块碰撞点
            val blockHit = rayTraceBlocks(
                level,
                ClipContext(startVec, fullEndVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this),
                if (this.isPenetrating()) Predicate { true } else Predicate { false }
            ).takeIf { it.type != HitResult.Type.MISS }

            // 2. 在路径上查找实体（仅在方块碰撞点之前）
            val searchEnd = blockHit?.location ?: fullEndVec
            val entityResults = findEntitiesOnPath(startVec, searchEnd)

            // 3. 找出最近的单一命中目标（方块或实体，取距离最近者，与原版行为一致）
            var closestHit: HitResult? = blockHit
            var closestDist = blockHit?.let { startVec.distanceToSqr(it.location) } ?: Double.MAX_VALUE

            for (entityResult in entityResults) {
                val entity = entityResult.entity
                val shooter = this.owner
                // 跳过无法伤害的玩家
                if (entity is Player && shooter is Player && !shooter.canHarmPlayer(entity)) continue
                if (entity == shooter || entity == shooter?.vehicle) continue

                val dist = startVec.distanceToSqr(entityResult.hitVec)
                if (dist < closestDist) {
                    closestDist = dist
                    closestHit = ExtendedEntityRayTraceResult(entityResult)
                }
            }

            // 4. 仅对最近的命中目标调用一次 onHit（与原版 ThrowableProjectile 一致）
            if (closestHit != null) {
                this.onHit(closestHit)
                // 命中后将位置设置到碰撞点，确保反弹等逻辑从正确的表面开始
                val hitLoc = closestHit.location
                this.setPos(hitLoc.x, hitLoc.y, hitLoc.z)
            }
        }
        // 客户端位置由 ClientMotionSyncMessage 每 tick 同步，不再自行推算

        val vec = this.deltaMovement
        val posX = this.x + vec.x
        val posY = this.y + vec.y
        val posZ = this.z + vec.z
        // 更新朝向（在 deltaMovement 可能被 onHit/反弹 修改之后）
        this.updateRotation()

        // 5. 对当前 deltaMovement（已包含反弹等修改）施加摩擦力和重力
        val friction = if (this.isInWater) 0.8 else 1.0
        this.deltaMovement = vec.scale(friction)

        this.deltaMovement = this.deltaMovement.add(0.0, -this.getCustomGravity().toDouble(), 0.0)

        this.setPos(posX, posY, posZ)

        if (this.tickCount > lifeValue) {
            if (explosionRadiusValue > 0) {
                causeExplode(position())
            }
            this.discard()
        }

        if (!this.isFastMoving && this.isFastMoving() && this.level().isClientSide) {
            playFlySound.accept(this)
            playNearFlySound.accept(this)
        }
        this.isFastMoving = this.isFastMoving()

        // 同步动量与位置到客户端
        this.syncMotion()

        // 更新区块加载位置
        if (level() is ServerLevel) {
            if (forceLoadChunk() && ProjectileConfig.PROJECTILE_CHUNK_LOADING.get()) {
//                this.keepChunkLoaded(this.position())
//                this.keepChunkLoaded(position().add(this.deltaMovement.normalize().scale(16.0)))
                ChunkLoadManager.updateEntityChunks(
                    this,
                    setOf(
                        position(),
                        position().add(this.deltaMovement.normalize().scale(16.0)),
                        position().add(this.deltaMovement)
                    )
                )
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

    public override fun onHit(result: HitResult) {
        if (result is BlockHitResult) {
            val level = this.level()
            if (result.type == HitResult.Type.MISS) {
                return
            }
            val resultPos = result.blockPos
            val state = level.getBlockState(resultPos)
            val event = state.block.getSoundType(state, level, resultPos, this).breakSound

            val hitVec = result.location
            level.playSound(
                null,
                hitVec.x,
                hitVec.y,
                hitVec.z,
                event,
                SoundSource.AMBIENT,
                1f,
                1f
            )

            this.level().gameEvent(
                GameEvent.PROJECTILE_LAND,
                hitVec,
                GameEvent.Context.of(this, state)
            )

            this.onHitBlock(result)
        }

        if (result is ExtendedEntityRayTraceResult) {
            val entity = result.entity

            if (this.owner is Player) {
                if (entity.hasIndirectPassenger(this.owner!!)) {
                    return
                }
            }

            this.level().gameEvent(
                GameEvent.PROJECTILE_LAND,
                result.location,
                GameEvent.Context.of(this, null)
            )

            this.onHitEntity(result)
        }
    }

    public override fun onHitEntity(result: EntityHitResult) {
        if (result !is ExtendedEntityRayTraceResult) return

        var entity = result.entity ?: return
        val headshot = result.headshot
        val legShot = result.legShot

        if (postEvent(HitEntity(this.owner, this, result))) return

        if (entity is PartEntity<*>) {
            entity = entity.getParent()
        }

        if (entity is LivingEntity) {
            if (isBeast()) {
                beastKill(this.owner, entity)
                return
            }
        }

        val shooter = this.owner
        if (headshot) {
            if (shooter is ServerPlayer) {
                val holder = Holder.direct(ModSounds.HEADSHOT.get())
                sendPacketTo(
                    shooter, ClientboundSoundPacket(
                        holder,
                        SoundSource.PLAYERS,
                        shooter.x,
                        shooter.y,
                        shooter.z,
                        1f,
                        1f,
                        shooter.level().random.nextLong()
                    )
                )
                sendPacketTo(shooter, ClientIndicatorMessage(1, 5))
            }
            performOnHit(entity, this.damageValue, true, this.getKnockback().toDouble())
        } else {
            if (shooter is ServerPlayer) {
                val holder = Holder.direct(ModSounds.INDICATION.get())
                sendPacketTo(
                    shooter, ClientboundSoundPacket(
                        holder,
                        SoundSource.PLAYERS,
                        shooter.x,
                        shooter.y,
                        shooter.z,
                        1f,
                        1f,
                        shooter.level().random.nextLong()
                    )
                )
                sendPacketTo(shooter, ClientIndicatorMessage(0, 5))
            }

            if (legShot) {
                if (entity is LivingEntity) {
                    if (entity is Player && entity.isCreative) {
                        return
                    }
                    if (!entity.level().isClientSide()) {
                        entity.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false))
                    }
                }
                this.damageValue *= this.getLegShot()
            }

            performOnHit(entity, this.damageValue, false, this.getKnockback().toDouble())
        }

        this.afterHitEntity(result)
    }

    public override fun onHitBlock(result: BlockHitResult) {
        val level = this.level()
        val pos = result.blockPos
        val face = result.direction
        val state = level.getBlockState(pos)
        val location = result.location
        if (postEvent(HitBlock(pos, state, face, this.owner, this, location))) return
        state.onProjectileHit(level, state, result, this)

        this.afterHitBlock(result)
    }

    open fun afterHitEntity(result: EntityHitResult) {
        if (this.explosionDamageValue > 0) {
            this.causeExplode(result.location)
            this.causeRangedEffects(result.location)
        }
        this.discard()
    }

    open fun afterHitBlock(result: BlockHitResult) {
        if (this.explosionDamageValue > 0) {
            this.causeExplode(result.location)
            this.causeRangedEffects(result.location)
        }
        this.discard()
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
                if (isHeadshot)
                    ModDamageTypes.causeProjectileHitHeadshotDamage(this.level().registryAccess(), this, this.owner)
                else
                    ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.owner),
                damage * headShotModifier
            )
            entity.invulnerableTime = 0
        }
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
            .beast(this.isBeast())
    }

    open fun causeRangedEffects(vec3: Vec3) {
        if (this.owner == null) return
        if (this.level() is ServerLevel) {
            val entities = SeekTool.Builder(this.owner!!)
                .withinRange(vec3, explosionRadiusValue.toDouble())
                .notItsVehicle()
                .baseFilter()
                .noVehicle()
                .build()

            entities.asSequence()
                .filter { it is LivingEntity && !(it is Player && it.isCreative) }
                .forEach { entity ->
                    val dis = vec3.distanceTo(entity.position())
                    if (!checkNoClip(entity, vec3)) return@forEach

                    this.getEffects().forEach {
                        val instance = MobEffectInstance(
                            it.effect,
                            (it.duration * (dis / explosionRadiusValue)).toInt(),
                            it.amplifier,
                            it.isAmbient,
                            it.isVisible,
                            it.showIcon()
                        )
                        (entity as LivingEntity).addEffect(instance, this.owner)
                    }
                }
        }
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

    open fun discardAfterExplode(): Boolean {
        return false
    }

    open fun keepChunkLoaded(position: Vec3) {
        ChunkLoadManager.updateEntityChunks(this, setOf(position))
//        val chunkPos = ChunkPos(BlockPos.containing(position))
//        (level() as ServerLevel).chunkSource.addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 3, this.id)
    }

    override fun isFastMoving(): Boolean {
        return this.deltaMovement.length() >= 0.5
    }

    override fun writeSpawnData(buffer: FriendlyByteBuf) {
        val motion = this.deltaMovement
        buffer.writeFloat(motion.x.toFloat())
        buffer.writeFloat(motion.y.toFloat())
        buffer.writeFloat(motion.z.toFloat())
    }

    override fun readSpawnData(additionalData: FriendlyByteBuf) {
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

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> {
        return NetworkHooks.getEntitySpawningPacket(this)
    }

    override fun shouldRenderAtSqrDistance(pDistance: Double): Boolean {
        return true
    }

    override fun setCustomGravity(gravity: Float) {
        this.gravityValue = gravity
    }

    override fun getCustomGravity(): Float {
        return this.gravityValue
    }

    open fun hugeMissileTrail() {
        if (level() is ServerLevel) {
            MissileTrailParticleMessage.sendToNearbyPlayers(
                level() as ServerLevel,
                xo, yo, zo,
                bbHeight.toDouble(),
                deltaMovement.x, deltaMovement.y, deltaMovement.z
            )
        }
    }

    open fun largeTrail() {
        if (level().isClientSide && tickCount > 2) {
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
                        160,
                        0.93f,
                        (10 + 8 * random).toInt(),
                        0.03f,
                        size = 0.75f
                    ), pos.x + random * 0.2, pos.y + random * 0.2, pos.z + random * 0.2, 0.0, 0.0, 0.0
                )
                i += 2
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
                val random = 2 * (this.random.nextFloat() - 0.5f)
                level().addParticle(
                    CustomFlareOption(
                        0.5f,
                        0.43f,
                        0.36f,
                        160,
                        0.93f,
                        (10 + 8 * random).toInt(),
                        0.03f,
                        size = 0.4f
                    ), pos.x + random * 0.125, pos.y + random * 0.125, pos.z + random * 0.125, 0.0, 0.0, 0.0
                )
                i += 1.5
            }
        }
    }

    open fun shellTrail() {
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
                val random = 2 * (this.random.nextFloat() - 0.5f)
                level().addParticle(
                    CustomFlareOption(
                        0.5f,
                        0.43f,
                        0.36f,
                        80,
                        0.9f,
                        (10 + 8 * random).toInt(),
                        0.01f,
                        size = 0.25f
                    ), pos.x + random * 0.1, pos.y + random * 0.1, pos.z + random * 0.1, 0.0, 0.0, 0.0
                )
                i += 1
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

    open fun bounce(direction: Direction) {
        val speed = this.deltaMovement.length()
        if (speed < 0.15) {
            this.deltaMovement = Vec3.ZERO
            return
        }

        when (direction.axis) {
            Direction.Axis.X -> this.deltaMovement = this.deltaMovement.multiply(-0.6, 0.8, 0.8)
            Direction.Axis.Y -> {
                this.deltaMovement = this.deltaMovement.multiply(0.8, -0.5, 0.8)
                if (this.deltaMovement.y() < this.getCustomGravity()) {
                    this.deltaMovement = this.deltaMovement.multiply(1.0, 0.0, 1.0)
                }
            }

            Direction.Axis.Z -> this.deltaMovement = this.deltaMovement.multiply(0.8, 0.8, -0.6)
        }
    }

    companion object {
        @JvmField
        val SYNCED_TICK: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(FastThrowableProjectile::class.java, EntityDataSerializers.INT)

        var playFlySound: Consumer<FastThrowableProjectile> = Consumer { }
        var playNearFlySound: Consumer<FastThrowableProjectile> = Consumer { }
    }

    /** 独立于原版 tickCount 的计时器，每 tick +1，通过 EntityData 持久化同步 */
    open var syncedTick by SYNCED_TICK

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(SYNCED_TICK, 0)
    }
}