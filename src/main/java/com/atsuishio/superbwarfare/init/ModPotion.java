package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class ModPotion {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(BuiltInRegistries.POTION, Mod.MODID);

    public static final DeferredHolder<Potion, Potion> SHOCK = POTIONS.register("superbwarfare_shock",
            () -> new Potion(new MobEffectInstance(ModMobEffects.SHOCK, 100, 0)));
    public static final DeferredHolder<Potion, Potion> STRONG_SHOCK = POTIONS.register("superbwarfare_strong_shock",
            () -> new Potion(new MobEffectInstance(ModMobEffects.SHOCK, 100, 1)));
    public static final DeferredHolder<Potion, Potion> LONG_SHOCK = POTIONS.register("superbwarfare_long_shock",
            () -> new Potion(new MobEffectInstance(ModMobEffects.SHOCK, 400, 0)));
}
