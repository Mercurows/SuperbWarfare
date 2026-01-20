package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.entity.Entity

object LongerWire : AmmoPerk("longer_wire", Type.AMMO) {
    override fun modifyProjectile(
        data: GunData,
        instance: PerkInstance,
        entity: Entity
    ) {
        if (entity is TaserBulletEntity) {
            entity.wireLength = instance.level.toInt()
        }
    }
}
