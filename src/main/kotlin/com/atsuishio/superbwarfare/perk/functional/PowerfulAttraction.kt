package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.living.LivingDropsEvent
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent
import net.minecraftforge.event.entity.living.LootingLevelEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object PowerfulAttraction : Perk("powerful_attraction", Type.FUNCTIONAL) {

    private const val ABSORBED = "sbw_absorbed_experience"

    @SubscribeEvent
    fun onLivingDrops(event: LivingDropsEvent) {
        val player = resolveAttacker(event.source) ?: return
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return

        val level = GunData.from(stack).perk.getLevel(this)
        if (level > 0 && isWeaponDamage(event.source)) {
            val drops = event.drops
            drops.forEach {
                val item = it.item
                if (!player.addItem(item.copy())) {
                    player.drop(item, false)
                }
            }
            event.isCanceled = true
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onLivingDeath(event: LivingDeathEvent) {
        if (event.isCanceled) return
        val entity = event.entity
        if (entity is Player) return
        if (!entity.shouldDropExperience() || entity.wasExperienceConsumed()) return
        val source = event.source
        if (!isWeaponDamage(source)) return

        val player = resolveAttacker(source) ?: return
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return

        val level = GunData.from(stack).perk.getLevel(this)
        if (level <= 0) return

        val experience = entity.getExperienceReward()
        if (experience <= 0) return

        entity.skipDropExperience()
        entity.persistentData.putBoolean(ABSORBED, true)
        player.giveExperiencePoints((experience * (0.8f + 0.2f * level)).toInt())
    }

    @SubscribeEvent
    fun onLivingExperienceDrop(event: LivingExperienceDropEvent) {
        val entity = event.entity
        if (entity.persistentData.getBoolean(ABSORBED)) {
            event.isCanceled = true
            return
        }

        val player = event.attackingPlayer ?: resolveAttacker(entity.lastDamageSource) ?: return
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return

        val level = GunData.from(stack).perk.getLevel(this)
        if (level > 0 && isWeaponDamage(entity.lastDamageSource)) {
            player.giveExperiencePoints((event.droppedExperience * (0.8f + 0.2f * level)).toInt())
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onLootingLevel(event: LootingLevelEvent) {
        val source = event.damageSource ?: return
        val sourceEntity = source.entity
        if (sourceEntity !is LivingEntity) return

        val stack = sourceEntity.mainHandItem
        if (stack.item !is GunItem) return

        val level = GunData.from(stack).perk.getLevel(this)
        if (level > 0 && isWeaponDamage(source)) {
            event.lootingLevel = level / 4
        }
    }

    private fun isWeaponDamage(source: DamageSource?): Boolean {
        if (source == null) return false
        return DamageTypeTool.isGunDamage(source) || DamageTypeTool.isMeleeDamage(source)
    }

    private fun resolveAttacker(source: DamageSource?): Player? {
        if (source == null) return null
        for (entity in listOfNotNull(source.entity, source.directEntity)) {
            if (entity is Player) return entity
            val owner = when (entity) {
                is Projectile -> entity.owner
                is OwnableEntity -> entity.owner
                else -> null
            }
            if (owner is Player) return owner
            if (owner is OwnableEntity) (owner.owner as? Player)?.let { return it }
        }
        return null
    }
}
