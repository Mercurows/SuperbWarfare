package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.entity.multipartRoot
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.forceApplyEffect
import com.atsuishio.superbwarfare.tools.postEvent
import com.atsuishio.superbwarfare.world.phys.EntityResult
import com.atsuishio.superbwarfare.world.phys.ExtendedEntityRayTraceResult
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

abstract class PerkAwareProjectile(type: EntityType<out Projectile>, level: Level) :
    Projectile(type, level), IBulletProperties {

    internal class ShotPerks(val data: GunData, val instances: Map<Perk.Type, List<PerkInstance>>)

    internal var shotPerks: ShotPerks? = null
        private set

    var currentHitDamage: Float? = null
        private set

    private val ammoEffects = linkedSetOf<MobEffectInstance>()

    internal fun captureShotPerks(data: GunData) {
        shotPerks = ShotPerks(data, GunData.PERK_TYPES.associateWith { data.perk.getInstances(it).toList() })
    }

    override fun setEffects(effects: List<MobEffectInstance>) {
        ammoEffects.addAll(effects)
    }

    override fun getEffects(): Set<MobEffectInstance> = ammoEffects

    open fun applyAmmoEffects(target: Entity, attacker: Entity? = owner) {
        val living = target.multipartRoot as? LivingEntity ?: return
        for (effect in ammoEffects) {
            living.forceApplyEffect(MobEffectInstance(effect), attacker)
        }
    }

    open fun dispatchEntityHit(
        target: Entity,
        point: Vec3,
        damage: Float,
        headshot: Boolean = false,
        attacker: Entity? = owner
    ): Boolean {
        val result = ExtendedEntityRayTraceResult(EntityResult(target.multipartRoot, point, headshot, legShot = false))
        val previousDamage = currentHitDamage
        currentHitDamage = damage
        return try {
            !postEvent(ProjectileHitEvent.HitEntity(attacker, this, result))
        } finally {
            currentHitDamage = previousDamage
        }
    }

    open fun dispatchBlockHit(hit: BlockHitResult, attacker: Entity? = owner): Boolean =
        !postEvent(
            ProjectileHitEvent.HitBlock(
                hit.blockPos, level().getBlockState(hit.blockPos), hit.direction, attacker, this, hit.location
            )
        )
}
