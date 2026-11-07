package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage
import com.atsuishio.superbwarfare.network.message.receive.LightningArcMessage
import com.atsuishio.superbwarfare.tools.*
import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.*
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.player.Player
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.registries.ForgeRegistries
import java.util.*

/**
 * Proxy that exposes Entity properties and methods to JS perk scripts.
 */
@Suppress("unused")
class EntityProxy(val entity: Entity?) {
    fun getId(): Int = entity?.id ?: -1

    fun getUUID(): String = entity?.stringUUID ?: ""

    /** Resolve only loaded entities in this dimension; never load chunks for a stored mark. */
    fun findByUUID(uuid: String): EntityProxy {
        val level = entity?.level() as? ServerLevel ?: return EntityProxy(null)
        val id = try {
            UUID.fromString(uuid)
        } catch (_: IllegalArgumentException) {
            return EntityProxy(null)
        }
        return EntityProxy(level.getEntity(id))
    }

    fun isAlive(): Boolean = entity?.isAlive == true

    fun isMob(): Boolean = entity is Mob

    /** Cosmetic confirmation for hits that deliberately bypass the damage path. */
    fun showHitFeedback() {
        val player = entity as? ServerPlayer ?: return
        sendPacketTo(player, ClientboundSoundPacket(
            Holder.direct(ModSounds.INDICATION.get()), SoundSource.PLAYERS,
            player.x, player.y, player.z, 1f, 1f, player.level().random.nextLong()
        ))
        sendPacketTo(player, ClientIndicatorMessage(0, 5))
    }

    fun setAttackTarget(other: EntityProxy) {
        val mob = entity as? Mob ?: return
        val target = other.entity as? LivingEntity ?: return
        if (mob.level().isClientSide || mob.level() !== target.level() || mob === target) return
        if (mob.isAlive && target.isAlive) mob.target = target
    }

    fun distanceToSqr(other: EntityProxy): Double =
        entity?.let { e -> other.entity?.let { e.distanceToSqr(it) } } ?: Double.MAX_VALUE

    fun getNearbyEnemies(attacker: EntityProxy, radius: Double): Array<EntityProxy> {
        val e = entity ?: return emptyArray()
        if (e.level().isClientSide || !radius.isFinite() || radius <= 0) return emptyArray()
        val range = radius.coerceAtMost(32.0)
        val owner = attacker.entity
        return e.level().getEntitiesOfClass(LivingEntity::class.java, e.boundingBox.inflate(range)) {
            it !== e && it !== owner && it.isAlive && !it.isSpectator
                && !(it is Player && it.isCreative)
                && !SeekTool.IS_FRIENDLY.test(owner, it)
                && (owner == null || !owner.isPassengerOfSameVehicle(it))
                && it.distanceToSqr(e) <= range * range && e is LivingEntity && e.hasLineOfSight(it)
        }.sortedBy { e.distanceToSqr(it) }.take(128).map { EntityProxy(it) }.toTypedArray()
    }

    fun shock(damage: Float, duration: Int, amplifier: Int, attacker: EntityProxy) {
        val e = entity as? LivingEntity ?: return
        val level = e.level() as? ServerLevel ?: return
        if (!e.isAlive || e.isSpectator || (e is Player && e.isCreative)) return
        if (damage > 0f) {
            e.invulnerableTime = 0
            DamageHandler.doDamage(
                e, ModDamageTypes.causeShockDamage(level.registryAccess(), attacker.entity), damage
            )
        }
        if (e is Creeper) {
            e.thunderHit(level, LightningBolt(EntityType.LIGHTNING_BOLT, level))
        } else if (duration > 0) {
            e.forceApplyEffect(
                MobEffectInstance(ModMobEffects.SHOCK.get(), duration, amplifier.coerceAtLeast(0)), attacker.entity
            )
        }
    }

    fun lightningTo(target: EntityProxy) {
        val start = entity ?: return
        val end = target.entity ?: return
        val level = start.level() as? ServerLevel ?: return
        val from = start.boundingBox.center
        val to = end.boundingBox.center
        sendPacketTo(PacketDistributor.NEAR.with {
            PacketDistributor.TargetPoint(from.x, from.y, from.z, 96.0, level.dimension())
        }, LightningArcMessage(from.x, from.y, from.z, to.x, to.y, to.z, level.random.nextLong()))
    }

    // ── Type checks ──
    fun isNull(): Boolean = entity == null
    fun isPlayer(): Boolean = entity is Player
    fun isLivingEntity(): Boolean = entity is LivingEntity
    fun isProjectile(): Boolean = entity is ProjectileEntity

    fun isZoom(): Boolean = (entity as? ProjectileEntity)?.isZoom() ?: false

    // ── Status effects ──
    fun setSecondsOnFire(seconds: Int) {
        val e = entity ?: return
        if (!e.level().isClientSide && seconds > 0) e.setSecondsOnFire(seconds)
    }

    fun addEffect(effectId: String, duration: Int, amplifier: Int, source: EntityProxy): Boolean {
        val e = entity as? LivingEntity ?: return false
        if (e.level().isClientSide || duration <= 0) return false
        val id = ResourceLocation.tryParse(effectId) ?: return false
        val effect = ForgeRegistries.MOB_EFFECTS.getValue(id) ?: return false
        return e.addEffect(MobEffectInstance(effect, duration, amplifier.coerceAtLeast(0)), source.entity)
    }

    /** Vanilla freezing thaws by two ticks per tick outside powder snow. */
    fun freeze(duration: Int) {
        val e = entity as? LivingEntity ?: return
        if (e.level().isClientSide || !e.canFreeze() || duration <= 0) return
        e.ticksFrozen = maxOf(e.ticksFrozen, e.ticksRequiredToFreeze + 2 * duration)
    }

    // ── Creative / Inventory ──
    fun isCreative(): Boolean = (entity as? Player)?.isCreative == true

    fun hasCreativeAmmoBox(): Boolean = InventoryTool.hasCreativeAmmoBox(entity)

    // ── Health ──
    fun getHealth(): Float = (entity as? LivingEntity)?.health ?: 0f

    fun getMaxHealth(): Float = (entity as? LivingEntity)?.maxHealth ?: 0f

    fun heal(amount: Float) {
        (entity as? LivingEntity)?.heal(amount)
    }

    fun getAbsorption(): Float = (entity as? LivingEntity)?.absorptionAmount ?: 0f

    fun setAbsorption(amount: Float) {
        val e = entity as? LivingEntity ?: return
        e.absorptionAmount = amount
    }

    /**
     * Absorb overflow healing as absorption health.
     * Used by HealClip postReload.
     */
    fun absorbExtraHealth(healAmount: Float, absorptionRate: Float) {
        val e = entity as? LivingEntity ?: return
        val absorption = healAmount - e.maxHealth + e.health
        if (absorption > 0) {
            e.absorptionAmount = absorption * absorptionRate
        }
    }

    /**
     * Heal nearby allied players within range.
     * Used by HealClip postReload.
     */
    fun healNearbyAllies(range: Double, amount: Float) {
        val e = entity as? LivingEntity ?: return
        e.level().getEntitiesOfClass(
            Player::class.java,
            e.boundingBox.inflate(range)
        )
            .filter { it.isAlliedTo(e) || (e is OwnableEntity && e.owner == it) }
            .forEach { it.heal(amount) }
    }

    // ── Alliance ──
    fun isAlliedTo(other: EntityProxy): Boolean {
        val e = entity as? LivingEntity ?: return false
        val o = other.entity as? LivingEntity ?: return false
        return o.isAlliedTo(e)
    }

    fun findLookingEntity(maxDistance: Double): EntityProxy {
        val lookedAt = TraceTool.findLookingEntity(entity, maxDistance)
        return EntityProxy(lookedAt)
    }

    fun isSameAs(other: EntityProxy): Boolean {
        return entity != null && entity == other.entity
    }

    /**
     * Check if this entity is ownable and is the same as the target entity.
     * Used by FieldDoctor for self-owned entity checks.
     */
    fun isOwnableAndEquals(target: EntityProxy): Boolean {
        return entity is OwnableEntity && entity == target.entity
    }

    // ── Projectile ──
    fun getBypassArmorRate(): Double {
        return (entity as? ProjectileEntity)?.getBypassArmorRate()?.toDouble() ?: 0.0
    }

    // ── Explosion ──
    fun createExplosion(damage: Number, radius: Number, attackerProxy: EntityProxy, keepBlocks: Boolean, fireTime: Number) {
        val target = entity ?: return
        val attacker = attackerProxy.entity ?: return
        val builder = CustomExplosion.Builder(target)
            .damage(damage.toFloat())
            .radius(radius.toFloat())
            .directSource(attacker)
            .source(null)
            .fireTime(fireTime.toInt())
        if (keepBlocks) {
            builder.keepBlock()
        }

        builder.explode()
    }

    // ── Motion / Velocity ──
    fun getFallDistance(): Float = entity?.fallDistance ?: 0f

    fun resetFallDistance() {
        val e = entity ?: return
        if (!e.level().isClientSide) e.resetFallDistance()
    }

    fun push(x: Double, y: Double, z: Double) {
        val e = entity ?: return
        if (e is ServerPlayer) {
            val newMotion = e.deltaMovement.add(x, y, z)
            sendPacketTo(e, ClientMotionSyncMessage(e.id, newMotion))
        } else {
            e.deltaMovement = e.deltaMovement.add(x, y, z)
            e.hurtMarked = true
        }
    }

    fun pushForward(strength: Double) {
        val e = entity as? LivingEntity ?: return
        val look = e.lookAngle
        push(look.x * strength, look.y * strength, look.z * strength)
    }
}
