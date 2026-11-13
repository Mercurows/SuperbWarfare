package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent
import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.entity.multipartRoot
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ReforgingLightningArcMessage
import com.atsuishio.superbwarfare.tools.*
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.entity.PartEntity
import net.minecraftforge.network.NetworkHooks
import java.util.function.Consumer

open class ReforgingLightningBallEntity(type: EntityType<out ReforgingLightningBallEntity>, level: Level) :
    Projectile(type, level), IBulletProperties, BasicGeoProjectileEntity {

    private val animation = if (level.isClientSide) BasicProjectileAnimationInstance(this, true) else null

    override fun getAnimationInstance() = animation

    private var damage = 6f
    private var explosionDamage = 6f
    private var explosionRadius = 8f
    private var explosionPenetrationRatio: Float = CustomExplosion.DEFAULT_PENETRATION_RATIO
    private var life = 100
    private var age = 0
    private var attackInterval = 10
    private var maxTargets = 4
    private var detonated = false
    var volt: Int = 0
        set(value) { field = value.coerceAtLeast(0) }

    /** Damage of the contact/discharge currently being dispatched to onHit scripts. */
    var currentHitDamage: Float? = null
        private set

    // Populated only by server-to-client discharge notifications; never used for damage.
    val lightningArcs = mutableListOf<LightningArc>()

    class LightningArc(val end: Vec3, val seed: Long, var ticks: Int = 4)

    init {
        noPhysics = true
        isNoGravity = true
        noCulling = true
    }

    override fun defineSynchedData() {}

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> =
        NetworkHooks.getEntitySpawningPacket(this)

    override fun getDamage() = damage
    override fun setDamage(value: Float) {
        damage = value.coerceAtLeast(0f)
    }

    override fun getExplosionDamage() = explosionDamage
    override fun setExplosionDamage(value: Float) {
        explosionDamage = value.coerceAtLeast(0f)
    }

    override fun getExplosionRadius() = explosionRadius
    override fun setExplosionRadius(value: Float) {
        explosionRadius = value.coerceIn(0f, 32f)
    }

    override fun getExplosionPenetrationRatio() = explosionPenetrationRatio
    override fun setExplosionPenetrationRatio(value: Float) {
        explosionPenetrationRatio = value.coerceIn(0f, CustomExplosion.MAX_PENETRATION_RATIO)
    }

    override fun getLife() = life
    override fun setLife(value: Int) {
        life = value.coerceAtLeast(1)
    }

    override fun isPenetrating() = true
    override fun getUnderwaterMotionScale() = 1f

    @Deprecated("Deprecated in Java")
    override fun isPushedByFluid() = false

    override fun tick() {
        super.tick()
        age++
        if (!level().isClientSide && age >= life) {
            discard()
            return
        }

        // Block collisions, gravity and fluid drag are ignored; swept entity contacts still deal damage.
        val previousPosition = position()
        setPos(previousPosition.add(deltaMovement))

        if (level().isClientSide) {
            if (tickCount == 1) playFlySound.accept(this)
            lightningArcs.removeAll { --it.ticks <= 0 }
            if (tickCount % 2 == 0) {
                level().addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    x + (random.nextDouble() - 0.5) * 1.8,
                    y + (random.nextDouble() - 0.5) * 1.8,
                    z + (random.nextDouble() - 0.5) * 1.8,
                    0.0, 0.0, 0.0
                )
            }
        } else {
            contactDamage(previousPosition, position())
            if (age % attackInterval == 0) discharge()
        }
    }

    private fun canAffect(target: Entity): Boolean {
        val root = target.multipartRoot
        val shooter = owner
        return root is LivingEntity && root.isAlive && target.isAlive && !root.isSpectator && root !== shooter
                && !(root is Player && root.isCreative)
                && (target is PartEntity<*> || !target.isMultipartEntity)
                && (target !is PartEntity<*> || target.canBeHitByProjectile())
                && !SeekTool.IS_FRIENDLY.test(shooter, root)
                && (shooter == null || !shooter.isPassengerOfSameVehicle(root))
    }

    private fun contactDamage(start: Vec3, end: Vec3) {
        if (getDamage() <= 0f) return
        val halfWidth = bbWidth * 0.5
        val halfHeight = bbHeight * 0.5
        // Forge's untyped query includes parts even when their parent lies outside this box.
        val targets = level().getEntities(this, AABB(start, end).inflate(halfWidth, halfHeight, halfWidth), ::canAffect)
            .filter { touchesTarget(it.boundingBox, start, end, halfWidth, halfHeight) }
            .sortedBy { it.boundingBox.distanceToSqr(start) }
            .distinctBy { it.multipartRoot.id }
        for (target in targets) {
            hit(target, getDamage())
        }
    }

    private fun discharge() {
        val attackRadius = getExplosionRadius().toDouble()
        if (attackRadius <= 0.0 || getExplosionDamage() <= 0f) return
        val targets = level().getEntities(this, AABB(position(), position()).inflate(attackRadius), ::canAffect)
            .filter { it.boundingBox.distanceToSqr(position()) <= attackRadius * attackRadius }
            .sortedBy { it.boundingBox.distanceToSqr(position()) }
            .distinctBy { it.multipartRoot.id }
            .take(maxTargets)

        if (targets.isEmpty()) return
        for (target in targets) {
            hit(target, getExplosionDamage())
        }
        level().playSound(null, x, y, z, ModSounds.LIGHTNING_BALL_ATTACK.get(), SoundSource.PLAYERS, 3f, 1f)
    }

    private fun hit(target: Entity, damage: Float) {
        val root = target.multipartRoot as? LivingEntity ?: return
        currentHitDamage = damage
        try {
            if (postEvent(ProjectileHitEvent.HitEntity(owner, this, root, target.boundingBox.center))) return
        } finally {
            currentHitDamage = null
        }
        root.invulnerableTime = 0
        target.invulnerableTime = 0
        // Preserve native part-specific damage routing, but apply the effect to the living root.
        target.forceHurt(ModDamageTypes.causeShockDamage(level().registryAccess(), this, owner), damage)
        root.forceApplyEffect(MobEffectInstance(ModMobEffects.SHOCK.get(), 100 + volt * 30, volt), owner)
        dischargeVisual(target)
    }

    private fun dischargeVisual(target: Entity) {
        val end = target.boundingBox.center
        sendPacketToTrackingThis(ReforgingLightningArcMessage(id, end.x, end.y, end.z, random.nextLong()))
    }

    override fun remove(reason: RemovalReason) {
        // Real destruction (including lifetime expiry) detonates once; chunk unloads never detonate.
        if (!level().isClientSide && reason.shouldDestroy() && !isRemoved && !detonated) {
            detonated = true
            if (getExplosionRadius() > 0f && getExplosionDamage() > 0f) {
                // CustomExplosion reaches twice its radius parameter. Contact/chain hits on the
                // previous tick must not consume this final shock explosion through hurt cooldown.
                val blastReach = getExplosionRadius() * 2.0
                for (target in level().getEntities(this, AABB(position(), position()).inflate(blastReach)) {
                    it.multipartRoot is LivingEntity
                }) {
                    if (target.distanceToSqr(position()) <= blastReach * blastReach) {
                        target.multipartRoot.invulnerableTime = 0
                        target.invulnerableTime = 0
                    }
                }
                CustomExplosion.Builder(this)
                    .damageSource(ModDamageTypes.causeShockDamage(level().registryAccess(), this, owner))
                    .damage(getExplosionDamage())
                    .radius(getExplosionRadius())
                    .position(position())
                    .keepBlock()
                    .withParticleType(ParticleTool.ParticleType.MINI)
                    .explode()
                (level() as? ServerLevel)?.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 24, 1.0, 1.0, 1.0, 0.2)
            }
        }
        super.remove(reason)
    }

    fun addLightningArc(end: Vec3, seed: Long) {
        if (level().isClientSide) {
            if (lightningArcs.size >= MAX_VISIBLE_ARCS) lightningArcs.removeAt(0)
            lightningArcs.add(LightningArc(end, seed))
        }
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        isNoGravity = true
        if (tag.contains("Damage")) setDamage(tag.getFloat("Damage"))
        if (tag.contains("ExplosionDamage")) setExplosionDamage(tag.getFloat("ExplosionDamage"))
        if (tag.contains("ExplosionRadius")) setExplosionRadius(tag.getFloat("ExplosionRadius"))
        else if (tag.contains("AttackRadius")) setExplosionRadius(tag.getFloat("AttackRadius"))
        if (tag.contains("Life")) setLife(tag.getInt("Life"))
        if (tag.contains("Age")) age = tag.getInt("Age").coerceAtLeast(0)
        if (tag.contains("AttackInterval")) attackInterval = tag.getInt("AttackInterval").coerceAtLeast(1)
        if (tag.contains("MaxTargets")) maxTargets = tag.getInt("MaxTargets").coerceIn(1, 32)
        if (tag.contains("Volt")) volt = tag.getInt("Volt")
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        tag.putFloat("Damage", damage)
        tag.putFloat("ExplosionDamage", getExplosionDamage())
        tag.putFloat("ExplosionRadius", getExplosionRadius())
        tag.putInt("Life", life)
        tag.putInt("Age", age)
        tag.putInt("AttackInterval", attackInterval)
        tag.putInt("MaxTargets", maxTargets)
        tag.putInt("Volt", volt)
    }

    companion object {
        private const val MAX_VISIBLE_ARCS = 16

        var playFlySound: Consumer<ReforgingLightningBallEntity> = Consumer { }

        private fun touchesTarget(
            bounds: AABB,
            start: Vec3,
            end: Vec3,
            halfWidth: Double,
            halfHeight: Double
        ): Boolean {
            val inflated = bounds.inflate(halfWidth, halfHeight, halfWidth)
            return inflated.contains(start) || inflated.clip(start, end).isPresent
        }
    }
}
