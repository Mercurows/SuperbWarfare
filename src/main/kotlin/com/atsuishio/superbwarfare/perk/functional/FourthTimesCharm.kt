package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity

object FourthTimesCharm : Perk("fourth_times_charm", Type.FUNCTIONAL) {
    override fun tick(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        data.perk.reduceCooldown(this, "FourthTimesCharmTick")
        val tag = data.perk.getTag(this) ?: return
        val count = tag.getInt("FourthTimesCharmCount")

        if (count >= 4) {
            tag.remove("FourthTimesCharmTick")
            tag.remove("FourthTimesCharmCount")

            val mag = data.get(GunProp.MAGAZINE)
            if (mag > 0) {
                data.ammo.set(mag.coerceAtMost(data.ammo.get() + 2))
            } else {
                data.virtualAmmo.add(2)
            }
        }
    }

    override fun onHurtEntity(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        super.onHurtEntity(damage, data, instance, target, source)
        val projectile = source.directEntity
        if (projectile is ProjectileEntity) {
            val bypassArmorRate = projectile.getBypassArmorRate()
            if (bypassArmorRate >= 1 && source.`is`(ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE)) {
                handleFourthTimesCharm(data, instance)
            } else if (source.`is`(ModDamageTypes.GUN_FIRE_HEADSHOT)) {
                handleFourthTimesCharm(data, instance)
            }
        }
    }

    fun handleFourthTimesCharm(data: GunData, instance: PerkInstance) {
        val tag = data.perk.getTag(this) ?: return
        val fourthTimesCharmTick = tag.getInt("FourthTimesCharmTick")
        if (fourthTimesCharmTick <= 0) {
            tag.putInt("FourthTimesCharmTick", 40 + 10 * instance.level)
            tag.putInt("FourthTimesCharmCount", 1)
        } else {
            val count = tag.getInt("FourthTimesCharmCount")
            if (count < 4) {
                tag.putInt("FourthTimesCharmCount", 4.coerceAtMost(count + 1))
            }
        }
    }
}
