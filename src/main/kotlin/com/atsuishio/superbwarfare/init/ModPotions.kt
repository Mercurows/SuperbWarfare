package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.item.alchemy.Potion
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModPotions {
    val POTIONS: DeferredRegister<Potion> = DeferredRegister.create(ForgeRegistries.POTIONS, Mod.MODID)

    @JvmField
    val SHOCK =
        registerPotion("superbwarfare_shock") { Potion(MobEffectInstance(ModMobEffects.SHOCK.get(), 100, 0)) }

    @JvmField
    val STRONG_SHOCK =
        registerPotion("superbwarfare_strong_shock") { Potion(MobEffectInstance(ModMobEffects.SHOCK.get(), 100, 1)) }

    @JvmField
    val LONG_SHOCK =
        registerPotion("superbwarfare_long_shock") { Potion(MobEffectInstance(ModMobEffects.SHOCK.get(), 400, 0)) }

    private fun registerPotion(id: String, potion: () -> Potion): RegistryObject<Potion> {
        return POTIONS.register(id, potion)
    }

    fun register(bus: IEventBus) {
        POTIONS.register(bus)
    }
}