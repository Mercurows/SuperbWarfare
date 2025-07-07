package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModPotions {

    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, Mod.MODID);

    public static final RegistryObject<Potion> SHOCK = POTIONS.register("superbwarfare_shock",
            () -> new Potion(new MobEffectInstance(ModMobEffects.SHOCK.get(), 100, 0)));
    public static final RegistryObject<Potion> STRONG_SHOCK = POTIONS.register("superbwarfare_strong_shock",
            () -> new Potion(new MobEffectInstance(ModMobEffects.SHOCK.get(), 100, 1)));
    public static final RegistryObject<Potion> LONG_SHOCK = POTIONS.register("superbwarfare_long_shock",
            () -> new Potion(new MobEffectInstance(ModMobEffects.SHOCK.get(), 400, 0)));
}
