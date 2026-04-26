package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.entity.Entity

object MicroMissile : AmmoPerk(Builder("micro_missile", Type.AMMO).speedRate(1.2)) {
    override fun modifyProjectile(
        data: GunData,
        instance: PerkInstance,
        entity: Entity
    ) {
        entity.isNoGravity = true
    }

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        with(GunProp) {
            modifier[EXPLOSION_DAMAGE] *= (0.8 + modifier.data.perk.getLevel(this@MicroMissile) * 0.1)
            modifier[EXPLOSION_RADIUS] *= 0.5
            modifier[GRAVITY] = 0.0
        }
    }
}
