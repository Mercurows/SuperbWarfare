package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import kotlin.math.pow

object JHPBullet : AmmoPerk(
    Builder("jhp_bullet", Type.AMMO).bypassArmorRate(-0.2).damageRate(1.1).speedRate(0.95).slug().rgb(230, 131, 65)
) {
    override fun getModifiedDamage(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ): Float {
        val armor = if (target is LivingEntity) target.getAttributeValue(Attributes.ARMOR) else 0.0
        return damage * (1.0f + 0.15f * instance.level) * ((400 / ((armor - 5.0).coerceAtLeast(0.0)
            .pow(4.0) + 400)).toFloat() + 0.2f)
    }
}
