package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity

object KillingTally : Perk("killing_tally", Type.DAMAGE) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData {
        val tag = data.perk.getTag(this) ?: return super.computeProperties(data, rawData)
        rawData.damage *= 1 + (0.1 * data.perk.getLevel(this)) * tag.getInt("KillingTally")
        return super.computeProperties(data, rawData)
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
