package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity

class FairMeans : Perk("fair_means", Type.DAMAGE) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData {
        val tag = data.perk.getTag(this) ?: return super.computeProperties(data, rawData)
        if (tag.getBoolean("FairMeans")) {
            rawData.damage *= 1.5 + 0.225 * data.perk.getLevel(this)
        } else {
            rawData.damage *= 0.2 + 0.04 * data.perk.getLevel(this)
        }
        return super.computeProperties(data, rawData)
    }

    override fun onHurtEntity(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val tag = data.perk.getTag(this) ?: return
        if (data.get(GunProp.BYPASSES_ARMOR) > 0) {
            if (source.`is`(ModTags.DamageTypes.PROJECTILE_ABSOLUTE)) {
                tag.putBoolean("FairMeans", !tag.getBoolean("FairMeans"))
            }
        } else if (source.`is`(ModTags.DamageTypes.PROJECTILE)) {
            tag.putBoolean("FairMeans", !tag.getBoolean("FairMeans"))
        }
    }
}
