package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import kotlin.math.floor

object OneTwoPunch : Perk("one_two_punch", Type.DAMAGE) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        val tag = modifier.data.perk.getTag(this) ?: return
        if (tag.getInt("OneTwoPunchTime") > 0) {
            modifier[GunProp.MELEE_DAMAGE] *= (1.5 + 0.75 * (modifier.data.perk.getLevel(this) - 1))
        }
    }

    override fun onHit(
        attacker: LivingEntity,
        data: GunData,
        instance: PerkInstance,
        target: Entity
    ) {
        val tag = data.perk.getTag(this) ?: return
        val count = if (tag.getInt("OneTwoPunchCountTime") > 0) tag.getInt("OneTwoPunchCount") + 1 else 1
        val needCount = floor(data.get(GunProp.PROJECTILE_AMOUNT) * (1 - 0.05 * (instance.level - 1)))
            .toInt().coerceAtLeast(1)

        if (count >= needCount) {
            data.perk.putStructuralInt(this, "OneTwoPunchTime", 60)
            tag.remove("OneTwoPunchCount")
            tag.remove("OneTwoPunchCountTime")
        } else {
            tag.putInt("OneTwoPunchCount", count)
            tag.putInt("OneTwoPunchCountTime", 2)
        }
    }

    override fun onChangeSlot(
        data: GunData,
        instance: PerkInstance,
        living: Entity?
    ) {
        clearOneTwoPunch(data)
    }

    override fun onMeleeDamage(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource,
    ) {
        if (damage > 0f) clearOneTwoPunch(data)
    }

    private fun clearOneTwoPunch(data: GunData) {
        val tag = data.perk.getTag(this) ?: return
        val wasActive = tag.getInt("OneTwoPunchTime") > 0
        tag.remove("OneTwoPunchTime")
        tag.remove("OneTwoPunchCount")
        tag.remove("OneTwoPunchCountTime")
        if (wasActive) data.invalidateProperties()
    }

    override fun tick(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        data.perk.reduceCooldown(this, "OneTwoPunchTime")
        data.perk.reduceCooldown(this, "OneTwoPunchCountTime")
        val tag = data.perk.getTag(this) ?: return
        if (!tag.contains("OneTwoPunchCountTime")) tag.remove("OneTwoPunchCount")
    }
}
