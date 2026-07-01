package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.InventoryTool
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player

/**
 * Proxy that exposes Entity properties and methods to JS perk scripts.
 */
class EntityProxy(val entity: Entity?) {

    // ── Type checks ──
    fun isNull(): Boolean = entity == null
    fun isPlayer(): Boolean = entity is Player
    fun isLivingEntity(): Boolean = entity is LivingEntity
    fun isProjectile(): Boolean = entity is ProjectileEntity

    fun isZoom(): Boolean = (entity as? ProjectileEntity)?.isZoom() ?: false

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
