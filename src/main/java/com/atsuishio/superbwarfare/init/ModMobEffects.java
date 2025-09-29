package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.mobeffect.BurnMobEffect;
import com.atsuishio.superbwarfare.mobeffect.ShockMobEffect;
import com.atsuishio.superbwarfare.mobeffect.StrikeProtectionMobEffect;
import com.atsuishio.superbwarfare.mobeffect.TraumaMobEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Mod.MODID);

    public static final DeferredHolder<MobEffect, ShockMobEffect> SHOCK = REGISTRY.register("shock", ShockMobEffect::new);
    public static final DeferredHolder<MobEffect, BurnMobEffect> BURN = REGISTRY.register("burn", BurnMobEffect::new);
    public static final DeferredHolder<MobEffect, StrikeProtectionMobEffect> STRIKE_PROTECTION = REGISTRY.register("strike_protection", StrikeProtectionMobEffect::new);
    public static final DeferredHolder<MobEffect, TraumaMobEffect> TRAUMA = REGISTRY.register("trauma", TraumaMobEffect::new);
}
