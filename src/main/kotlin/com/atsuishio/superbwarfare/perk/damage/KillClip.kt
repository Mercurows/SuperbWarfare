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

object KillClip : Perk("kill_clip", Type.DAMAGE) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        val tag = modifier.data.perk.getTag(this) ?: return
        if (tag.getInt("KillClipTime") > 0) {
            modifier[GunProp.DAMAGE] *= (1.2 + 0.05 * modifier.data.perk.getLevel(this))
        }
    }

    override fun tick(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        data.perk.reduceCooldown(this, "KillClipReloadTime")
        data.perk.reduceCooldown(this, "KillClipTime")
    }

    override fun preReload(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        val tag = data.perk.getTag(this) ?: return
        val time = tag.getInt("KillClipReloadTime")
        if (time > 0) {
            tag.remove("KillClipReloadTime")
            tag.putBoolean("KillClip", true)
        } else {
            tag.remove("KillClip")
        }
    }

    override fun postReload(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        val tag = data.perk.getTag(this) ?: return
        if (!tag.getBoolean("KillClip")) return
        val level = instance.level
        tag.putInt("KillClipTime", 90 + 10 * level)
    }

    override fun onKill(
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val tag = data.perk.getTag(this) ?: return
        if (DamageTypeTool.isGunDamage(source)) {
            val killClipLevel = instance.level.toInt()
            if (killClipLevel != 0) {
                tag.putInt("KillClipReloadTime", 80)
            }
        }
    }
}
