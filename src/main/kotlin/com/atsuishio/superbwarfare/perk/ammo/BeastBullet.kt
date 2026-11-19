package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.entity.Entity

object BeastBullet : AmmoPerk(Builder("beast_bullet", Type.AMMO).bypassArmorRate(0.0).rgb(134, 65, 14)) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        modifier[GunProp.PROJECTILE_PENETRATING] = true
    }

    override fun modifyProjectile(
        data: GunData,
        instance: PerkInstance,
        entity: Entity
    ) {
        super.modifyProjectile(data, instance, entity)
        if (entity !is IBulletProperties) return
        entity.setBeast(true)
    }
}
