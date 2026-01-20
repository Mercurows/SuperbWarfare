package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.tags.EntityTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.Vex

object RiotBullet : AmmoPerk(
    Builder("riot_bullet", Type.AMMO).bypassArmorRate(-0.3).damageRate(0.9).speedRate(0.8).slug().rgb(70, 35, 230)
        .mobEffect { MobEffects.MOVEMENT_SLOWDOWN }.mobEffect { MobEffects.WEAKNESS }
) {
    override fun getModifiedDamage(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ): Float {
        if (target.type.`is`(EntityTypeTags.RAIDERS) || target is Vex) {
            return damage * (1 + 0.5f * instance.level)
        }
        return super.getModifiedDamage(damage, data, instance, target, source)
    }

    override fun getEffectAmplifier(instance: PerkInstance): Int {
        return instance.level / 4
    }

    override fun getEffectDuration(instance: PerkInstance): Int {
        return 20 + instance.level * 10
    }
}
