package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity

object MagnificentHowl : Perk("magnificent_howl", Type.DAMAGE) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData {
        val tag = data.perk.getTag(this) ?: return super.computeProperties(data, rawData)
        if (tag.getInt("MagnificentHowlDamageCount") > 0) {
            rawData.damage *= 1.5
        }
        return super.computeProperties(data, rawData)
    }

    override fun onKill(
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val tag = data.perk.getTag(this) ?: return
        if (DamageTypeTool.isHeadshotDamage(source)) {
            tag.putInt(
                "MagnificentHowlCount",
                (tag.getInt("MagnificentHowlCount") + 1 + instance.level / 5).coerceAtMost(9 + instance.level)
            )
        }
    }

    override fun preReload(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        val tag = data.perk.getTag(this) ?: return
        tag.putInt("MagnificentHowlDamageCount", tag.getInt("MagnificentHowlCount"))
        tag.remove("MagnificentHowlCount")
    }

    override fun onHurtEntity(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val tag = data.perk.getTag(this) ?: return
        if (tag.getInt("MagnificentHowlDamageCount") > 0) {
            data.perk.reduceCooldown(this, "MagnificentHowlDamageCount")
        }
    }
}
