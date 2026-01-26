package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.item.alchemy.Potion
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModPotions {
    val POTIONS: DeferredRegister<Potion> = DeferredRegister.create(BuiltInRegistries.POTION, Mod.MODID)

    @JvmField
    val SHOCK =
        registerPotion("superbwarfare_shock") { Potion(MobEffectInstance(ModMobEffects.SHOCK, 100, 0)) }

    @JvmField
    val STRONG_SHOCK =
        registerPotion("superbwarfare_strong_shock") { Potion(MobEffectInstance(ModMobEffects.SHOCK, 100, 1)) }

    @JvmField
    val LONG_SHOCK =
        registerPotion("superbwarfare_long_shock") { Potion(MobEffectInstance(ModMobEffects.SHOCK, 400, 0)) }

    private fun registerPotion(id: String, potion: () -> Potion): DeferredHolder<Potion, out Potion> {
        return POTIONS.register(id, potion)
    }

    fun register(bus: IEventBus) {
        POTIONS.register(bus)
    }
}