package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity

object GutshotStraight : Perk("gutshot_straight", Type.DAMAGE) {
    override fun getModifiedDamage(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ): Float {
        val entity = source.directEntity
        if (DamageTypeTool.isGunFireDamage(source) && entity is ProjectileEntity && entity.isZoom()) {
            return damage * (1.15f + 0.05f * instance.level)
        }
        return super.getModifiedDamage(damage, data, instance, target, source)
    }
}
