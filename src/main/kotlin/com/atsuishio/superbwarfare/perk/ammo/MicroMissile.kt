package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.entity.Entity

class MicroMissile : AmmoPerk(Builder("micro_missile", Type.AMMO).speedRate(1.2)) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData {
        rawData.explosionDamage *= 0.8 + data.perk.getLevel(this) * 0.1
        rawData.explosionRadius *= 0.5
        rawData.gravity = 0.0
        return super.computeProperties(data, rawData)
    }

    override fun modifyProjectile(
        data: GunData,
        instance: PerkInstance,
        entity: Entity
    ) {
        entity.isNoGravity = true
    }
}
