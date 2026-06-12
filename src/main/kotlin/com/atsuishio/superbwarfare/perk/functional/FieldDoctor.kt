package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.OwnableEntity

object FieldDoctor : Perk("field_doctor", Type.FUNCTIONAL) {
    override fun onHurtEntity(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        super.onHurtEntity(damage, data, instance, target, source)
        if (!trigger(target, source)) {
            return
        }
        if (target is LivingEntity) {
            target.heal(damage * 1.0f.coerceAtMost(0.25f + 0.05f * instance.level))
        }
    }

    fun trigger(target: Entity?, source: DamageSource): Boolean {
        target ?: return false

        val directEntity = source.directEntity
        val sourceEntity = source.entity
        if (directEntity is ProjectileEntity && !directEntity.isZoom()) {
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
            return target.isAlliedTo(attacker) || (attacker is OwnableEntity && attacker == target)
        }
        return false
    }
}
