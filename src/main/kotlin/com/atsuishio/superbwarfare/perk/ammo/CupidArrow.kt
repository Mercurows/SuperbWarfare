package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.entity.mixin.CupidLove
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.google.common.collect.ImmutableList
import com.mojang.datafixers.util.Pair
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.schedule.Activity

object CupidArrow : AmmoPerk(Builder("cupid_arrow", Type.AMMO).damageRate(0.0).slug().rgb(255, 185, 215)) {
    override fun onHit(
        attacker: LivingEntity,
        data: GunData,
        instance: PerkInstance,
        target: Entity
    ) {
        val perkLevel = instance.level
        val list = target.level().getEntities(null, target.boundingBox.inflate(perkLevel * 0.25)) { it is AgeableMob }
        list.forEach {
            if (it is Animal && it.canFallInLove() && attacker is Player) {
                it.setInLove(attacker)
            }
            if (it is Villager && !it.isBaby) {
                val love = CupidLove.getInstance(it)
                love.`superbwarfare$setCupidLove`(true)

                if (it.canBreed()) {
                    it.brain.setActiveActivityIfPossible(Activity.IDLE)
                    it.brain.addActivity(Activity.IDLE, ImmutableList.of(Pair.of(1, VillagerMakeLove())))
                }
            }
            if (perkLevel >= 10) {
                if (it is AgeableMob && it.isBaby) {
                    it.ageUp(
                        AgeableMob.getSpeedUpSecondsWhenFeeding(-it.getAge()) * (1.0.coerceAtLeast(perkLevel - 10.0) / 5).toInt(),
                        true
                    )
                }
            }
            val level = it.level()
            if (level is ServerLevel) {
                val d0 = level.random.nextGaussian() * 0.02
                val d1 = level.random.nextGaussian() * 0.02
                val d2 = level.random.nextGaussian() * 0.02
                ParticleTool.sendParticle(
                    level,
                    ParticleTypes.HEART,
                    it.getRandomX(1.0),
                    it.randomY + 0.5,
                    it.getRandomZ(1.0),
                    5,
                    d0,
                    d1,
                    d2,
                    0.1,
                    false
                )
            }
        }
    }
}
