package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity

object KillingTally : Perk("killing_tally", Type.DAMAGE) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        val tag = modifier.data.perk.getTag(this) ?: return
        modifier[GunProp.DAMAGE] *= 1 + (0.1 * modifier.data.perk.getLevel(this)) * tag.getInt("KillingTally")
    }

    override fun preReload(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        data.perk.getTag(this)?.remove("KillingTally")
    }

    override fun onKill(
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val tag = data.perk.getTag(this) ?: return
        if (DamageTypeTool.isGunDamage(source)) {
            tag.putInt("KillingTally", 3.coerceAtMost(tag.getInt("KillingTally") + 1))
        }
    }

    override fun onChangeSlot(
        data: GunData,
        instance: PerkInstance,
        living: Entity?
    ) {
        data.perk.getTag(this)?.remove("KillingTally")
    }
}
