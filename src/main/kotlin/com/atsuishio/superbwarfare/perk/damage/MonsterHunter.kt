package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.Monster

object MonsterHunter : Perk("monster_hunter", Type.DAMAGE) {
    override fun getModifiedDamage(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ): Float {
        return if (target is Monster) {
            damage * (1.1f + 0.1f * instance.level)
        } else super.getModifiedDamage(
            damage,
            data,
            instance,
            target,
            source
        )
    }
}
