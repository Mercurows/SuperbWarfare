package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile

object Firefly : Perk("firefly", Type.DAMAGE) {
    override fun onKill(
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        if (!DamageTypeTool.isHeadshotDamage(source)) return
        val sourceEntity = source.entity
        val attacker: Player =
            sourceEntity as? Player
                ?: if (sourceEntity is Projectile && sourceEntity.owner is Player) {
                    sourceEntity.owner as Player
                } else {
                    return
                } ?: return

        CustomExplosion.Builder(target)
            .damage(6 + instance.level * 2f)
            .radius(2 + instance.level * 0.5f)
            .directSource(attacker)
            .source(null)
            .keepBlock()
            .fireTime(3 + instance.level / 3)
            .explode()
    }
}
