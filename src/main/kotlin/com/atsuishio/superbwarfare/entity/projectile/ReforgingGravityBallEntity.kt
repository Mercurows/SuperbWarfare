package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.tools.*
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.HangingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.*
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.network.NetworkHooks
import org.joml.Vector3f
import java.util.*
import java.util.function.Consumer
import kotlin.math.sqrt

open class ReforgingGravityBallEntity(type: EntityType<out ReforgingGravityBallEntity>, level: Level) :
    Projectile(type, level), IBulletProperties, BasicGeoProjectileEntity {

    private val animation = if (level.isClientSide) BasicProjectileAnimationInstance(this, true) else null
    private var damageValue = 12f
    private var explosionDamageValue = 80f
    private var explosionPenetrationRatioValue: Float = CustomExplosion.DEFAULT_PENETRATION_RATIO
    private var projectileLife by PROJECTILE_LIFE
    private var age = 0
    private var gravityValue = 0.04f
    private var activationDelay = 10
    var collapseVisualPlayed = false
        private set

    var anchored by ANCHORED
        private set
    var anchorTime by ANCHOR_TIME
        private set
    var activationTime by ACTIVATION_TIME
        private set
    var expansionDuration by EXPANSION_DURATION
        private set
    val fieldDuration: Int
        get() = (getLife().toLong() - expansionDuration * 2L).coerceAtLeast(0L).toInt()
    private var damageInterval by DAMAGE_INTERVAL
    private var syncedAttackRadius by ATTACK_RADIUS
    private var syncedPullStrength by PULL_STRENGTH
    private var syncedOwnerUUID by OWNER_UUID

    val attackRadius: Double get() = getExplosionRadius().toDouble()
    val coreRadius: Double get() = attackRadius * 0.1
    val pullStrength: Double get() = syncedPullStrength.toDouble()

    fun getOwnerUUID(): UUID? = syncedOwnerUUID.orElse(null)

    private fun setOwnerUUID(uuid: UUID?) {
        syncedOwnerUUID = Optional.ofNullable(uuid)
    }

    override fun setOwner(pOwner: Entity?) {
        super.setOwner(pOwner)
        if (pOwner != null) setOwnerUUID(pOwner.uuid)
    }

    private val fieldEndTime: Long get() = anchorTime + getLife()
    private val collapseDuration: Int get() = (fieldEndTime - collapseStartTime).toInt()
    private val collapseStartTime: Long
        get() = (anchorTime + expansionDuration + fieldDuration).coerceAtMost(fieldEndTime)

    val collapsing: Boolean get() = anchored && level().gameTime >= collapseStartTime

    val fieldActive: Boolean
        get() = anchored && level().gameTime >= activationTime && level().gameTime < collapseStartTime

    private val fieldHolding: Boolean
        get() = anchored && level().gameTime >= activationTime && level().gameTime < fieldEndTime

    private val damageTick: Boolean
        get() {
            val interval = damageInterval.coerceAtLeast(2)
            return (level().gameTime - activationTime) % interval == (interval / 2).toLong()
        }

    fun expansionProgress(partialTicks: Float = 0f): Float =
        if (!anchored) 0f else {
            val elapsed = level().gameTime - anchorTime + partialTicks.toDouble()
            expansionFraction(elapsed, expansionDuration) * (1f - collapseProgress(partialTicks))
        }

    /** Synced world times also keep the orbit timer correct for late tracking clients. */
    fun orbitProgress(partialTicks: Float = 0f): Float =
        if (!anchored) 0f else {
            val start = (anchorTime + expansionDuration).coerceAtMost(collapseStartTime)
            val duration = (collapseStartTime - start).coerceAtLeast(1L)
            if (level().gameTime - collapseStartTime + partialTicks.toDouble() >= 0.0) 1f
            else ((level().gameTime - start + partialTicks.toDouble()) / duration).coerceIn(0.0, 1.0).toFloat()
        }

    private fun collapseProgress(partialTicks: Float = 0f): Float =
        if (!anchored) 0f else expansionFraction(
            level().gameTime - collapseStartTime + partialTicks.toDouble(),
            // Keep the restored singularity visible briefly before the final damage pulse.
            (collapseDuration - 2).coerceAtLeast(1)
        )

    init {
        noPhysics = true
        noCulling = true
    }

    override fun defineSynchedData() {
        with(entityData) {
            define(ANCHORED, false)
            define(ANCHOR_TIME, 0L)
            define(ACTIVATION_TIME, 0L)
            define(EXPANSION_DURATION, 20)
            define(PROJECTILE_LIFE, 200)
            define(DAMAGE_INTERVAL, 10)
            define(ATTACK_RADIUS, 8f)
            define(PULL_STRENGTH, 0.6f)
            define(OWNER_UUID, Optional.empty())
        }
    }

    override fun getAnimationInstance() = animation
    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> = NetworkHooks.getEntitySpawningPacket(this)
    override fun getDamage() = damageValue
    override fun setDamage(value: Float) {
        damageValue = value.coerceAtLeast(0f)
    }

    override fun getExplosionDamage() = explosionDamageValue
    override fun setExplosionDamage(value: Float) {
        explosionDamageValue = value.coerceAtLeast(0f)
    }

    override fun getExplosionRadius() = syncedAttackRadius
    override fun setExplosionRadius(value: Float) {
        syncedAttackRadius = value.coerceIn(0f, 32f)
    }

    override fun getExplosionPenetrationRatio() = explosionPenetrationRatioValue
    override fun setExplosionPenetrationRatio(value: Float) {
        explosionPenetrationRatioValue = value.coerceIn(0f, CustomExplosion.MAX_PENETRATION_RATIO)
    }

    override fun getLife() = projectileLife
    override fun setLife(value: Int) {
        projectileLife = value.coerceAtLeast(1)
    }

    override fun getCustomGravity() = gravityValue
    override fun setCustomGravity(gravity: Float) {
        gravityValue = gravity.coerceAtLeast(0f)
    }

    @Deprecated("Deprecated in Java")
    override fun isPushedByFluid() = false

    override fun tick() {
        super.tick()
        age++
        if (!level().isClientSide) {
            if (anchored && level().gameTime >= fieldEndTime) {
                collapseCore()
                discard()
                return
            }
            if (!anchored && age >= getLife()) {
                discard()
                return
            }
            if (collapsing && level().gameTime == collapseStartTime) {
                level().playSound(null, x, y, z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.8f)
            }
        }

        if (anchored) {
            deltaMovement = Vec3.ZERO
        } else {
            fly()
        }

        if (level().isClientSide) {
            tickClient()
        } else if (fieldHolding) {
            if (level().gameTime == activationTime) {
                level().playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 1.4f)
            }
            affectTargets()
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun tickClient() {
        if (fieldHolding) {
            val player = localPlayer
            if (player != null && player.level() === level() && canAffect(player)) {
                player.deltaMovement = attractedVelocity(player)
            }
        }
        if (tickCount % 2 != 0) return
        val progress = expansionProgress()
        val spread = if (fieldActive) 1.8 else 0.08 + progress * 1.4
        level().addParticle(
            DustParticleOptions(Vector3f(1f, 0.78f, 0.1f), 0.25f + progress * 0.4f),
            x + (random.nextDouble() - 0.5) * spread,
            y + (random.nextDouble() - 0.5) * spread,
            z + (random.nextDouble() - 0.5) * spread,
            0.0, 0.0, 0.0
        )
    }

    private fun fly() {
        val nextPosition = position().add(deltaMovement)
        if (!level().isClientSide) {
            val blockHit = level().clip(
                ClipContext(
                    position(),
                    nextPosition,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
                )
            )
            // Clip the entity sweep at the nearest block so targets behind walls cannot anchor the ball.
            val end = if (blockHit.type == HitResult.Type.BLOCK) blockHit.location else nextPosition
            val entityHit = ProjectileUtil.getEntityHitResult(
                this, position(), end, boundingBox.expandTowards(deltaMovement).inflate(1.0),
                { canHitEntity(it) }, position().distanceToSqr(end)
            )
            val hit = entityHit ?: blockHit
            if (hit.type != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
                onHit(hit)
                return
            }
        }
        setPos(nextPosition)
        deltaMovement = deltaMovement.add(0.0, -gravityValue.toDouble(), 0.0)
    }

    override fun onHitBlock(hit: BlockHitResult) {
        if (level().isClientSide || anchored) return
        super.onHitBlock(hit)
        val normal = Vec3.atLowerCornerOf(hit.direction.normal)
        anchorAt(hit.location.add(normal.scale(0.65)))
    }

    override fun onHitEntity(hit: EntityHitResult) {
        if (level().isClientSide || anchored) return
        super.onHitEntity(hit)
        anchorAt(hit.location)
    }

    private fun anchorAt(location: Vec3) {
        setPos(location)
        deltaMovement = Vec3.ZERO
        isNoGravity = true
        anchorTime = level().gameTime
        // ActivationDelay is a minimum wait; the field must also finish unfolding.
        activationTime = anchorTime + maxOf(activationDelay, expansionDuration)
        anchored = true
        hasImpulse = true
        level().playSound(null, x, y, z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.65f, 0.8f)
    }

    private fun shooter(): Entity? {
        val current = owner
        if (current != null && !current.isRemoved) return current
        val uuid = getOwnerUUID() ?: return null
        return level().getPlayerByUUID(uuid) ?: EntityFindUtil.findEntity(level(), uuid.toString())
    }

    /** Shared by server attraction, living-target damage and client-side player attraction. */
    fun canAffect(target: Entity): Boolean {
        val shooter = shooter()
        val shooterUUID = getOwnerUUID()
        return target !== this && target !is HangingEntity && target.isAlive && !target.isSpectator && target !== shooter
                && target !is ReforgingGravityBallEntity
                && (shooterUUID == null || target.uuid != shooterUUID)
                && !(target is Player && target.isCreative)
                && !SeekTool.IS_FRIENDLY.test(shooter, target)
                && (shooter == null || !shooter.isPassengerOfSameVehicle(target))
                && target.boundingBox.center.distanceToSqr(position()) <= attackRadius * attackRadius
    }

    private fun attractedVelocity(target: Entity): Vec3 = pullVelocity(
        target.deltaMovement, position().subtract(target.boundingBox.center), attackRadius, coreRadius, pullStrength
    )

    private fun affectTargets() {
        if (attackRadius <= 0.0) return
        val targets = level().getEntities(this, boundingBox.inflate(attackRadius), ::canAffect)
        if (targets.isEmpty()) return
        for (target in targets) {
            if (target is Projectile && canConsume(target.boundingBox, position(), coreRadius)) target.discard()
        }
        val regularDamage = fieldActive && damageTick
        val damagedTargets = targets.filterIsInstance<LivingEntity>().filter {
            regularDamage || canConsume(it.boundingBox, position(), coreRadius)
        }
        if (damagedTargets.isNotEmpty()) damageTargets(damagedTargets)
        attractTargets(targets)
    }

    private fun livingTargets(radius: Double): List<LivingEntity> {
        if (radius <= 0.0) return emptyList()
        return level().getEntitiesOfClass(
            LivingEntity::class.java,
            boundingBox.inflate(radius)
        ) { it.isAlive && canAffect(it) }
    }

    private fun damageTargets(targets: List<LivingEntity>, finalPulse: Boolean = false) {
        val source = ModDamageTypes.causeGravityDamage(level().registryAccess(), this, shooter())
        for (target in targets) {
            if (!target.isAlive) continue
            val damage = if (canConsume(target.boundingBox, position(), coreRadius)) {
                // Attempt a lethal hit through normal damage handling, preserving armor, hooks and death events.
                target.maxHealth
            } else if (finalPulse) {
                getExplosionDamage() * damageMultiplier(
                    target.boundingBox.distanceToSqr(position()).let { sqrt(it) },
                    attackRadius,
                    coreRadius
                )
            } else {
                damageValue * damageMultiplier(
                    target.boundingBox.center.distanceTo(position()),
                    attackRadius,
                    coreRadius
                )
            }
            if (damage <= 0f) continue

            val knockback = ICustomKnockback.getInstance(target)
            val previousStrength = knockback.`superbWarfare$getKnockbackStrength`()
            knockback.`superbWarfare$setKnockbackStrength`(0.0)
            try {
                target.invulnerableTime = 0
                target.forceHurt(source, damage)
            } finally {
                knockback.`superbWarfare$setKnockbackStrength`(previousStrength)
            }
        }
    }

    private fun collapseCore() {
        // Send before removal so tracking clients can keep the flash alive independently.
        level().broadcastEntityEvent(this, COLLAPSE_EVENT)
        val targets = livingTargets(attackRadius)
        if (targets.isNotEmpty()) {
            damageTargets(targets, finalPulse = true)
        }
        level().playSound(null, x, y, z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 0.8f, 1.7f)
    }

    override fun handleEntityEvent(event: Byte) {
        if (event == COLLAPSE_EVENT) {
            if (!collapseVisualPlayed) {
                collapseVisualPlayed = true
                playCollapseEffect.accept(this)
            }
        } else {
            super.handleEntityEvent(event)
        }
    }

    private fun attractTargets(targets: List<Entity>) {
        if (pullStrength <= 0.0) return
        for (target in targets) {
            if (!target.isAlive || target is Player) continue

            val motion = attractedVelocity(target)
            // Each entity's own tick decides whether/how velocity moves it, including stationary dummies.
            target.deltaMovement = motion
            target.hasImpulse = true
            target.hurtMarked = true
        }
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        if (tag.contains("Damage")) setDamage(tag.getFloat("Damage"))
        if (tag.contains("ExplosionDamage")) setExplosionDamage(tag.getFloat("ExplosionDamage"))
        if (tag.contains("Life")) setLife(tag.getInt("Life"))
        if (tag.contains("Age")) age = tag.getInt("Age").coerceAtLeast(0)
        if (tag.contains("Gravity")) setCustomGravity(tag.getFloat("Gravity"))
        if (tag.contains("ActivationDelay")) activationDelay = tag.getInt("ActivationDelay").coerceAtLeast(0)
        if (tag.contains("ExpansionDuration")) expansionDuration = tag.getInt("ExpansionDuration").coerceAtLeast(1)
        if (tag.contains("DamageInterval")) damageInterval = tag.getInt("DamageInterval").coerceAtLeast(2)
        if (tag.contains("ExplosionRadius")) setExplosionRadius(tag.getFloat("ExplosionRadius"))
        else if (tag.contains("AttackRadius")) setExplosionRadius(tag.getFloat("AttackRadius"))
        if (tag.contains("PullStrength")) syncedPullStrength = tag.getFloat("PullStrength").coerceIn(0f, 1f)
        if (tag.contains("Anchored")) anchored = tag.getBoolean("Anchored")
        if (tag.contains("ActivationTime")) activationTime = tag.getLong("ActivationTime")
        if (tag.contains("AnchorTime")) anchorTime = tag.getLong("AnchorTime")
        else if (anchored) anchorTime = activationTime - maxOf(activationDelay, expansionDuration)
        if (tag.hasUUID("Owner")) setOwnerUUID(tag.getUUID("Owner"))
        isNoGravity = anchored
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        tag.putFloat("Damage", damageValue)
        tag.putFloat("ExplosionDamage", getExplosionDamage())
        tag.putInt("Life", getLife())
        tag.putInt("Age", age)
        tag.putFloat("Gravity", gravityValue)
        tag.putInt("ActivationDelay", activationDelay)
        tag.putInt("ExpansionDuration", expansionDuration)
        tag.putInt("DamageInterval", damageInterval)
        tag.putFloat("ExplosionRadius", getExplosionRadius())
        tag.putFloat("PullStrength", pullStrength.toFloat())
        tag.putBoolean("Anchored", anchored)
        tag.putLong("AnchorTime", anchorTime)
        tag.putLong("ActivationTime", activationTime)
    }

    companion object {
        private const val COLLAPSE_EVENT: Byte = 67

        // Installed by the client renderer; dedicated servers never load client rendering classes.
        @JvmField
        var playCollapseEffect: Consumer<ReforgingGravityBallEntity> = Consumer { }

        private fun expansionFraction(elapsed: Double, duration: Int): Float {
            val progress = (elapsed / duration.coerceAtLeast(1)).coerceIn(0.0, 1.0)
            return (progress * progress * (3.0 - 2.0 * progress)).toFloat()
        }

        private fun damageMultiplier(distance: Double, radius: Double, coreRadius: Double): Float {
            if (radius <= 0 || distance > radius) return 0f
            val core = coreRadius.coerceIn(0.0, radius)
            if (distance <= core) return 1f
            val falloff = ((distance - core) / (radius - core)).coerceIn(0.0, 1.0)
            return (1.0 - EDGE_DAMAGE_LOSS * falloff).toFloat()
        }

        private fun canConsume(box: AABB, center: Vec3, coreRadius: Double): Boolean =
            coreRadius > 0.0 && maxOf(box.xsize, box.ysize, box.zsize) < coreRadius &&
                box.center.distanceToSqr(center) <= coreRadius * coreRadius

        private fun pullVelocity(current: Vec3, toCenter: Vec3, radius: Double, coreRadius: Double, strength: Double): Vec3 {
            val distance = toCenter.length()
            if (radius <= 0.0 || strength <= 0.0 || distance > radius) return current
            if (distance < 1.0E-4) return capSpeed(current.scale(CORE_DAMPING), MAX_PULL_SPEED)

            val core = coreRadius.coerceIn(0.0, radius)
            val direction = toCenter.scale(1.0 / distance)
            if (distance <= core) {
                return capSpeed(current.scale(CORE_DAMPING).add(direction.scale(CAPTURE_SPEED)), MAX_PULL_SPEED)
            }
            val proximity = ((radius - distance) / (radius - core).coerceAtLeast(1.0E-4)).coerceIn(0.0, 1.0)
            val inward = direction.scale(strength * (0.5 + 0.5 * proximity))
            return if (distance <= radius * FORCED_PULL_RADIUS_SCALE) {
                capSpeed(current.scale(PULL_MOMENTUM).add(inward), MAX_PULL_SPEED)
            } else {
                // Preserve existing tangential/outward momentum; only add gravity outside the inner 60%.
                current.add(inward)
            }
        }

        private fun capSpeed(motion: Vec3, max: Double): Vec3 {
            val speed = motion.length()
            return if (speed > max && speed > 0.0) motion.scale(max / speed) else motion
        }

        const val MAX_PULL_SPEED = 0.8

        private const val FORCED_PULL_RADIUS_SCALE = 0.6
        private const val PULL_MOMENTUM = 0.85
        private const val CORE_DAMPING = 0.35
        private const val CAPTURE_SPEED = 0.08
        private const val EDGE_DAMAGE_LOSS = 0.8

        @JvmField
        val ANCHORED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(ReforgingGravityBallEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val ANCHOR_TIME: EntityDataAccessor<Long> =
            SynchedEntityData.defineId(ReforgingGravityBallEntity::class.java, EntityDataSerializers.LONG)

        @JvmField
        val EXPANSION_DURATION: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ReforgingGravityBallEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val DAMAGE_INTERVAL: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ReforgingGravityBallEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val ACTIVATION_TIME: EntityDataAccessor<Long> =
            SynchedEntityData.defineId(ReforgingGravityBallEntity::class.java, EntityDataSerializers.LONG)

        @JvmField
        val PROJECTILE_LIFE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ReforgingGravityBallEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val ATTACK_RADIUS: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ReforgingGravityBallEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val PULL_STRENGTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ReforgingGravityBallEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val OWNER_UUID: EntityDataAccessor<Optional<UUID>> =
            SynchedEntityData.defineId(ReforgingGravityBallEntity::class.java, EntityDataSerializers.OPTIONAL_UUID)
    }
}
