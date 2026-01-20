package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import kotlin.math.pow

object VorpalWeapon : Perk("vorpal_weapon", Type.DAMAGE) {
    override fun getModifiedDamage(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ): Float {
        if (DamageTypeTool.isGunDamage(source) && target is LivingEntity && target.health >= 100.0f) {
            return (damage + target.health * 0.00002f * instance.level.toDouble().pow(2)).toFloat()
        }
        return super.getModifiedDamage(damage, data, instance, target, source)
    }
}
