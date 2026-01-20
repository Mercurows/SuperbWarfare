package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity

object HeadSeeker : Perk("head_seeker", Type.DAMAGE) {
    override fun getModifiedDamage(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ): Float {
        val tag = data.perk.getTag(this) ?: return super.getModifiedDamage(damage, data, instance, target, source)
        if (DamageTypeTool.isHeadshotDamage(source) && tag.getInt("HeadSeeker") > 0) {
            return damage * (1.095f + 0.0225f * instance.level)
        }
        return super.getModifiedDamage(damage, data, instance, target, source)
    }

    override fun tick(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        data.perk.reduceCooldown(this, "HeadSeeker")
    }

    override fun onHurtEntity(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val tag = data.perk.getTag(this) ?: return
        if (DamageTypeTool.isGunFireDamage(source)) {
            tag.putInt("HeadSeeker", 11 + instance.level * 2)
        }
    }
}
