package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.GunType
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import com.atsuishio.superbwarfare.tools.InventoryTool
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import kotlin.math.min

object Subsistence : Perk("subsistence", Type.FUNCTIONAL) {
    override fun onKill(
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val sourceEntity = source.entity
        val attacker: Player =
            sourceEntity as? Player
                ?: if (sourceEntity is Projectile && sourceEntity.owner is Player) {
                    sourceEntity.owner as Player
                } else {
                    return
                } ?: return

        if (DamageTypeTool.isGunDamage(source)) {
            val type = data.get(GunProp.GUN_TYPE)
            val rate = instance.level * (0.1f + if (type == GunType.SMG || type == GunType.RIFLE) 0.07f else 0f)

            PlayerVariable.modify(attacker) {
                val mag = data.get(GunProp.MAGAZINE)
                val ammo = data.ammo.get()
                val ammoReload = min(mag, (mag * rate).toInt())
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
}
