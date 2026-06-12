package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.entity.Entity

object PhasePenetratingBullet : AmmoPerk(
    Builder("phase_penetrating_bullet", Type.AMMO).speedRate(1.1).rgb(255, 255, 255)
) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        with(GunProp) {
            modifier[DAMAGE] *= (0.2 + 0.04 * modifier.data.perk.getLevel(this@PhasePenetratingBullet))
        }
    }

    override fun modifyProjectile(
        data: GunData,
        instance: PerkInstance,
        entity: Entity
    ) {
        super.modifyProjectile(data, instance, entity)
        if (entity !is ProjectileEntity) return
        entity.setPenetrating(true)
    }
}
