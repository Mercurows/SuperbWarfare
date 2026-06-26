package com.atsuishio.superbwarfare.advancement

import com.atsuishio.superbwarfare.advancement.criteria.OttoSprintTrigger
import com.atsuishio.superbwarfare.advancement.criteria.RPGMeleeExplosionTrigger
import com.atsuishio.superbwarfare.advancement.criteria.VehicleHurtTrigger
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.advancements.critereon.SimpleCriterionTrigger
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
object CriteriaRegister {
    lateinit var RPG_MELEE_EXPLOSION: RPGMeleeExplosionTrigger
    lateinit var OTTO_SPRINT: OttoSprintTrigger
    lateinit var VEHICLE_HURT: VehicleHurtTrigger

    @SubscribeEvent
    fun setup(event: FMLCommonSetupEvent) {
        event.enqueueWork {
            RPG_MELEE_EXPLOSION = register(RPGMeleeExplosionTrigger())
            OTTO_SPRINT = register(OttoSprintTrigger())
            VEHICLE_HURT = register(VehicleHurtTrigger())
        }
    }

    fun <T : SimpleCriterionTrigger<*>> register(criterion: T): T {
        CriteriaTriggers.register(criterion)
        return criterion
    }
}
