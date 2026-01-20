package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.InventoryTool
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

object CastNoShadows : Perk("cast_no_shadows", Type.FUNCTIONAL) {
    override fun onMeleeAttack(
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        super.onMeleeAttack(data, instance, target, source)

        val attacker = source.entity as? Player ?: return
        val rate = 0.2f + (instance.level - 1) * 0.03f
        attacker.heal(attacker.maxHealth * rate / 2f)

        PlayerVariable.modify(attacker) {
            val mag = data.get(GunProp.MAGAZINE)
            val ammo = data.ammo.get()
            val ammoReload = mag.coerceAtMost((mag * rate).toInt())
            val ammoNeed = (mag - ammo).coerceAtMost(ammoReload)

            val flag = attacker.isCreative || InventoryTool.hasCreativeAmmoBox(attacker)
            var ammoFinal = data.countBackupAmmo(attacker).coerceAtMost(ammoNeed)

            if (flag) {
                ammoFinal = ammoNeed
            } else {
                data.consumeBackupAmmo(attacker, ammoFinal)
            }
            data.ammo.set(mag.coerceAtMost(ammo + ammoFinal))
        }
    }
}
