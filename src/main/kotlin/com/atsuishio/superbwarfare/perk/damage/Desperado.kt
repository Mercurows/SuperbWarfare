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

object Desperado : Perk("desperado", Type.DAMAGE) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        val tag = modifier.data.perk.getTag(this) ?: return
        if (tag.getInt("DesperadoTimePost") > 0) {
            with(GunProp) {
                modifier[RPM] = (modifier[RPM] * (1.285 + 0.015 * modifier.data.perk.getLevel(this@Desperado))).toInt()
            }
        }
    }

    override fun tick(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        data.perk.reduceCooldown(this, "DesperadoTime")
        data.perk.reduceCooldown(this, "DesperadoTimePost")
    }

    override fun onKill(
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        if (DamageTypeTool.isHeadshotDamage(source)) {
            data.perk.getTag(this)?.putInt("DesperadoTime", 90 + instance.level * 10)
        }
    }

    override fun preReload(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        val tag = data.perk.getTag(this) ?: return
        val time = tag.getInt("DesperadoTime")
        if (time > 0) {
            tag.remove("DesperadoTime")
            tag.putBoolean("Desperado", true)
        } else {
            tag.remove("Desperado")
        }
    }

    override fun postReload(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        val tag = data.perk.getTag(this) ?: return
        if (!tag.getBoolean("Desperado")) return
        tag.putInt("DesperadoTimePost", 110 + instance.level * 10)
    }
}
