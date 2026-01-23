package com.atsuishio.superbwarfare.advancement

import com.atsuishio.superbwarfare.advancement.criteria.OttoSprintTrigger
import com.atsuishio.superbwarfare.advancement.criteria.RPGMeleeExplosionTrigger
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.SimpleCriterionTrigger
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
object CriteriaRegister {
    lateinit var RPG_MELEE_EXPLOSION: RPGMeleeExplosionTrigger
    lateinit var OTTO_SPRINT: OttoSprintTrigger

    @SubscribeEvent
    fun setup(event: FMLCommonSetupEvent) {
        event.enqueueWork {
            RPG_MELEE_EXPLOSION = register("rpg_melee_explosion", RPGMeleeExplosionTrigger())
            OTTO_SPRINT = register("otto_sprint", OttoSprintTrigger())
        }
    }

    fun <T : SimpleCriterionTrigger<*>> register(name: String, criterion: T): T {
        CriteriaTriggers.register(name, criterion)
        return criterion
    }
}
