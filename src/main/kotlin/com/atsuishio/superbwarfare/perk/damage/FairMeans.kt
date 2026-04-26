package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity

object FairMeans : Perk("fair_means", Type.DAMAGE) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        val tag = modifier.data.perk.getTag(this) ?: return
        with(GunProp) {
            if (tag.getBoolean("FairMeans")) {
                modifier[DAMAGE] *= (1.5 + 0.225 * modifier.data.perk.getLevel(this@FairMeans))
            } else {
                modifier[DAMAGE] *= (0.2 + 0.04 * modifier.data.perk.getLevel(this@FairMeans))
            }
        }
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
