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

    private const val FLAG = "FairMeans"
    private const val SWING_FLAG = "FairMeansSwing"
    private const val SWING_TICK = "FairMeansSwingTick"

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        val tag = modifier.data.perk.getTag(this) ?: return
        with(GunProp) {
            if (tag.getBoolean(FLAG)) {
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
                tag.putBoolean(FLAG, !tag.getBoolean(FLAG))
            }
        } else if (source.`is`(ModTags.DamageTypes.PROJECTILE)) {
            tag.putBoolean(FLAG, !tag.getBoolean(FLAG))
        }
    }

    override fun getModifiedMeleeDamage(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ): Float {
        val tag = data.perk.getTag(this) ?: return damage
        val tick = target.level().gameTime
        if (tag.getLong(SWING_TICK) != tick) {
            tag.putLong(SWING_TICK, tick)
            tag.putBoolean(SWING_FLAG, tag.getBoolean(FLAG))
            tag.putBoolean(FLAG, !tag.getBoolean(FLAG))
        }
        val multiplier = if (tag.getBoolean(SWING_FLAG)) {
            1.5 + 0.225 * instance.level
        } else {
            0.2 + 0.04 * instance.level
        }
        return (damage * multiplier).toFloat()
    }
}
