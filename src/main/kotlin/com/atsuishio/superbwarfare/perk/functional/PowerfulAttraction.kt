package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent

@EventBusSubscriber
object PowerfulAttraction : Perk("powerful_attraction", Type.FUNCTIONAL) {
    @SubscribeEvent
    fun onLivingDrops(event: LivingDropsEvent) {
        val source = event.source ?: return
        val sourceEntity = source.entity
        if (sourceEntity !is Player) return
        val stack = sourceEntity.mainHandItem
        if (stack.item !is GunItem) return

        val level = GunData.from(stack).perk.getLevel(this)
        if (level > 0 && (DamageTypeTool.isGunDamage(source) || source.`is`(DamageTypes.PLAYER_ATTACK))) {
            val drops = event.drops
            drops.forEach {
                val item = it.item
                if (!sourceEntity.addItem(item.copy())) {
                    sourceEntity.drop(item, false)
                }
            }
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onLivingExperienceDrop(event: LivingExperienceDropEvent) {
        val player = event.attackingPlayer ?: return
        val source = event.entity.lastDamageSource

        val stack = player.mainHandItem
        if (stack.item !is GunItem) return

        val level = GunData.from(stack).perk.getLevel(this)
        if (source != null && level > 0 && (DamageTypeTool.isGunDamage(source) || source.`is`(DamageTypes.PLAYER_ATTACK))) {
            player.giveExperiencePoints((event.droppedExperience * (0.8f + 0.2f * level)).toInt())
            event.isCanceled = true
        }
    }
}
