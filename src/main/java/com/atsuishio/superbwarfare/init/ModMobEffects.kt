package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.mobeffect.*
import net.minecraft.world.effect.MobEffect
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import java.util.function.Supplier

object ModMobEffects {
    val REGISTRY: DeferredRegister<MobEffect> = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Mod.MODID)

    fun <T : MobEffect> register(name: String, effect: T): RegistryObject<MobEffect> =
        REGISTRY.register(name, Supplier { effect })

    // @formatter:off
    @JvmField val SHOCK = register("shock", ShockMobEffect)
    @JvmField val BURN = register("burn", BurnMobEffect)

    @JvmField val STRIKE_PROTECTION = register("strike_protection", StrikeProtectionMobEffect)
    @JvmField val TRAUMA = register("trauma", TraumaMobEffect)

    @JvmField val PHOSPHORUS_FIRE = register("phosphorus_fire", PhosphorusFireMobEffect)
    // @formatter:on
}
