package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.mobeffect.BurnMobEffect;
import com.atsuishio.superbwarfare.mobeffect.ShockMobEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Mod.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> SHOCK = REGISTRY.register("shock", ShockMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BURN = REGISTRY.register("burn", BurnMobEffect::new);
}
