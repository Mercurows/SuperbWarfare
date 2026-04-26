package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.entity.Entity

object IncendiaryBullet : AmmoPerk(
    Builder("incendiary_bullet", Type.AMMO).bypassArmorRate(-0.4).damageRate(0.7).speedRate(0.75).slug()
        .rgb(230, 131, 65)
        .mobEffect(ModMobEffects.BURN)
) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        with(GunProp) {
            modifier[VELOCITY] = if (modifier.data.isShotgun) 4.5 else modifier[VELOCITY]
        }
    }

    override fun getEffectDuration(instance: PerkInstance): Int {
        return 60 + 20 * instance.level
    }

    override fun modifyProjectile(
        data: GunData,
        instance: PerkInstance,
        entity: Entity
    ) {
        super.modifyProjectile(data, instance, entity)
        if (entity !is ProjectileEntity) return
        entity.fireBullet(instance.level.toInt(), data.isShotgun)
    }
}
