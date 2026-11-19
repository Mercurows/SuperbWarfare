package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.entity.Entity

object FieldDoctor : Perk("field_doctor", Type.FUNCTIONAL) {
    override fun modifyProjectile(data: GunData, instance: PerkInstance, entity: Entity) {
        val projectile = entity as? IBulletProperties ?: return
        if (projectile.isZoom()) return

        val ratio = (0.25f + 0.05f * instance.level).coerceAtMost(1f)
        projectile.setAllyHealRatio(maxOf(projectile.getAllyHealRatio(), ratio))
    }
}
