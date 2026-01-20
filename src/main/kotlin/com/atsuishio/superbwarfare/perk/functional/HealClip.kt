package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player

object HealClip : Perk("heal_clip", Type.FUNCTIONAL) {
    override fun tick(data: GunData, instance: PerkInstance, entity: Entity?) {
        data.perk.reduceCooldown(this, "HealClipTime")
    }

    override fun onKill(
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val tag = data.perk.getTag(this) ?: return
        if (DamageTypeTool.isGunDamage(source)) {
            val healClipLevel = instance.level
            if (healClipLevel != 0.toShort()) {
                tag.putInt("HealClipTime", 80 + healClipLevel * 20)
            }
        }
    }

    override fun preReload(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        val tag = data.perk.getTag(this) ?: return
        val time = tag.getInt("HealClipTime")
        if (time > 0) {
            tag.remove("HealClipTime")
            tag.putBoolean("HealClip", true)
        } else {
            tag.remove("HealClip")
        }
    }

    override fun postReload(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        if (entity !is LivingEntity) return

        val tag = data.perk.getTag(this) ?: return
        if (tag.contains("HealClip")) {
            return
        }

        var healClipLevel = instance.level
        if (healClipLevel == 0.toShort()) {
            healClipLevel = 1
        }

        val healAmount = 12 * (0.8f + 0.2f * healClipLevel)
        val absorption = healAmount - entity.maxHealth + entity.health
        entity.heal(healAmount)
        if (absorption > 0) {
            entity.absorptionAmount = absorption * 0.3f
        }

        entity.level().getEntitiesOfClass(Player::class.java, entity.boundingBox.inflate(5.toDouble()))
            .filter { it.isAlliedTo(entity) || (entity is OwnableEntity && entity.owner == it) }
            .forEach { it.heal(6.0f * (0.8f + 0.2f * healClipLevel)) }
    }
}
