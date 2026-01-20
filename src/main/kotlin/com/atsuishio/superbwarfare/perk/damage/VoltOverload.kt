package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.entity.Entity

object VoltOverload : Perk("volt_overload", Type.DAMAGE) {
    override fun modifyProjectile(
        data: GunData,
        instance: PerkInstance,
        entity: Entity
    ) {
        if (entity is TaserBulletEntity) {
            entity.volt = instance.level.toInt()
        }
    }
}
