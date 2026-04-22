package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.advancement.criteria.OttoSprintTrigger
import com.atsuishio.superbwarfare.advancement.criteria.RPGMeleeExplosionTrigger
import net.minecraft.advancements.CriterionTrigger
import net.minecraft.core.registries.Registries
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModCriteriaTriggers {
    val REGISTRY: DeferredRegister<CriterionTrigger<*>> =
        DeferredRegister.create(Registries.TRIGGER_TYPE, Mod.MODID)

    @JvmField
    val RPG_MELEE_EXPLOSION: Supplier<RPGMeleeExplosionTrigger> =
        REGISTRY.register("rpg_melee_explosion", ::RPGMeleeExplosionTrigger)

    @JvmField
    val OTTO_SPRINT: Supplier<OttoSprintTrigger> =
        REGISTRY.register("otto_sprint", ::OttoSprintTrigger)
}
