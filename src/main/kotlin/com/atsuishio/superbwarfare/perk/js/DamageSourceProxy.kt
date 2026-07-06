package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile

/**
 * Proxy that exposes DamageSource properties and methods to JS perk scripts.
 */
class DamageSourceProxy(private val source: DamageSource) {

    // ── Damage type checks ──
    fun isGunDamage(): Boolean = DamageTypeTool.isGunDamage(source)

    fun isGunFireDamage(): Boolean = DamageTypeTool.isGunFireDamage(source)

    fun isHeadshot(): Boolean = source.`is`(ModDamageTypes.GUN_FIRE_HEADSHOT)

    fun isHeadshotDamage(): Boolean = DamageTypeTool.isHeadshotDamage(source)

    fun isAbsoluteHeadshot(): Boolean = source.`is`(ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE)

    fun isProjectile(): Boolean = source.`is`(ModTags.DamageTypes.PROJECTILE)

    fun isProjectileAbsolute(): Boolean = source.`is`(ModTags.DamageTypes.PROJECTILE_ABSOLUTE)

    // ── Entity access ──
    fun getDirectEntity(): EntityProxy = EntityProxy(source.directEntity)

    fun getSourceEntity(): EntityProxy = EntityProxy(source.entity)

    fun getEntity(): EntityProxy = EntityProxy(source.entity)

    /**
     * Get the attacking player from this damage source.
     * Handles both direct player attacks and projectile attacks owned by a player.
     * Used by Subsistence onKill.
     */
    fun getAttackingPlayer(): EntityProxy {
        val sourceEntity = source.entity
        val player = sourceEntity as? Player
            ?: if (sourceEntity is Projectile && sourceEntity.owner is Player) {
                sourceEntity.owner as Player
            } else {
                null
            }
        return EntityProxy(player)
    }

    /**
     * Get the attacking LivingEntity from this damage source.
     * Handles projectile ownership chains and ownable entity ownership.
     * Used by FieldDoctor onHurtEntity.
     */
    fun getAttacker(): EntityProxy {
        val sourceEntity = source.entity
        if (sourceEntity is LivingEntity) return EntityProxy(sourceEntity)

        val directEntity = source.directEntity
        if (directEntity is ProjectileEntity) {
            val owner = directEntity.owner
            if (owner is OwnableEntity && owner.owner is ServerPlayer) {
                return EntityProxy(owner.owner)
            } else if (owner is LivingEntity) {
                return EntityProxy(owner)
            }
        }

        return EntityProxy(null)
    }

    /**
     * Check if the direct entity is a non-zoom projectile.
     * Used by FieldDoctor trigger check.
     */
    fun isProjectileNotZoom(): Boolean {
        val direct = source.directEntity
        return direct is ProjectileEntity && !direct.isZoom()
    }

    /**
     * Check if damage should heal the target (FieldDoctor logic).
     * Encapsulates the complex alliance/ownership checking.
     */
    fun shouldHealTarget(target: EntityProxy): Boolean {
        val t = target.entity ?: return false
        val directEntity = source.directEntity
        val sourceEntity = source.entity

        if (directEntity !is ProjectileEntity || directEntity.isZoom()) return false

        var attacker: LivingEntity? = null
        if (sourceEntity is LivingEntity) {
            attacker = sourceEntity
        }

        val owner = directEntity.owner
        if (owner is OwnableEntity && owner.owner is ServerPlayer) {
            attacker = owner.owner
        } else if (owner is LivingEntity) {
            attacker = owner
        }

        attacker ?: return false
        return t.isAlliedTo(attacker) || (attacker is OwnableEntity && attacker == t)
    }
}
