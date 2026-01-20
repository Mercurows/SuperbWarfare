package com.atsuishio.superbwarfare.mobeffect

import com.atsuishio.superbwarfare.init.ModMobEffects
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraftforge.event.entity.living.LivingHealEvent
import net.minecraftforge.event.entity.living.LivingHurtEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object TraumaMobEffect : MobEffect(MobEffectCategory.HARMFUL, 0xF4ADB4) {
    @SubscribeEvent
    fun onLivingHeal(event: LivingHealEvent) {
        val entity = event.entity
        val effect = entity.getEffect(ModMobEffects.TRAUMA.get()) ?: return

        val amp = effect.amplifier + 1
        if (amp >= 10) {
            event.isCanceled = true
            return
        }

        val amount = event.amount
        event.amount = amount * (1 - amp * 0.1f)
    }

    @SubscribeEvent
    fun onLivingHurt(event: LivingHurtEvent) {
        val entity = event.entity
        val effect = entity.getEffect(ModMobEffects.TRAUMA.get()) ?: return

        val amp = effect.amplifier + 1
        val amount = event.amount
        event.amount = amount * (1 + amp * 0.15f)
    }
}