package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.advancement.criteria.OttoSprintTrigger;
import com.atsuishio.superbwarfare.advancement.criteria.RPGMeleeExplosionTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCriteriaTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> REGISTRY = DeferredRegister.create(Registries.TRIGGER_TYPE, Mod.MODID);

    public static final Supplier<RPGMeleeExplosionTrigger> RPG_MELEE_EXPLOSION = REGISTRY.register("rpg_melee_explosion", RPGMeleeExplosionTrigger::new);
    public static final Supplier<OttoSprintTrigger> OTTO_SPRINT = REGISTRY.register("otto_sprint", OttoSprintTrigger::new);
}
