package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.mobeffect.BurnMobEffect;
import com.atsuishio.superbwarfare.mobeffect.ShockMobEffect;
import com.atsuishio.superbwarfare.mobeffect.StrikeProtectionMobEffect;
import com.atsuishio.superbwarfare.mobeffect.TraumaMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Mod.MODID);

    public static final RegistryObject<MobEffect> SHOCK = REGISTRY.register("shock", ShockMobEffect::new);
    public static final RegistryObject<MobEffect> BURN = REGISTRY.register("burn", BurnMobEffect::new);
    public static final RegistryObject<MobEffect> STRIKE_PROTECTION = REGISTRY.register("strike_protection", StrikeProtectionMobEffect::new);
    public static final RegistryObject<MobEffect> TRAUMA = REGISTRY.register("trauma", TraumaMobEffect::new);
}
