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
        tag.putInt("OneTwoPunchCount", tag.getInt("OneTwoPunchCount") + 1)
        tag.putInt("OneTwoPunchCountTime", 2)

        val needCount = floor(data.get(GunProp.PROJECTILE_AMOUNT) * (1 - 0.05 * (instance.level - 1)))

        if (tag.getInt("OneTwoPunchCount") >= needCount) {
            tag.putInt("OneTwoPunchTime", 60)
            tag.remove("OneTwoPunchCount")
            tag.remove("OneTwoPunchCountTime")
        }
    }

    override fun onChangeSlot(
        data: GunData,
        instance: PerkInstance,
        living: Entity?
    ) {
        val tag = data.perk.getTag(this) ?: return
        tag.remove("OneTwoPunchTime")
        tag.remove("OneTwoPunchCount")
        tag.remove("OneTwoPunchCountTime")
    }

    override fun onMeleeAttack(
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val tag = data.perk.getTag(this) ?: return
        tag.remove("OneTwoPunchTime")
        tag.remove("OneTwoPunchCount")
        tag.remove("OneTwoPunchCountTime")
    }

    override fun tick(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        data.perk.reduceCooldown(this, "OneTwoPunchTime")
        data.perk.reduceCooldown(this, "OneTwoPunchCountTime")
    }
}
