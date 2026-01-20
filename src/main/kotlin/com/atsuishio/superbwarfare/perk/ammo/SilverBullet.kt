package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.tags.EntityTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

object SilverBullet : AmmoPerk(
    Builder("silver_bullet", Type.AMMO).bypassArmorRate(0.05).damageRate(0.8).speedRate(1.1).rgb(87, 166, 219)
) {
    override fun getModifiedDamage(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ): Float {
        if (target is LivingEntity && target.type.`is`(EntityTypeTags.UNDEAD)) {
            return damage * (1 + 0.5f * instance.level)
        }
        return super.getModifiedDamage(damage, data, instance, target, source)
    }
}
