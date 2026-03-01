package com.atsuishio.superbwarfare.mobeffect

import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacket
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.event.entity.living.LivingEvent
import net.minecraftforge.event.entity.living.MobEffectEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object BurnMobEffect : MobEffect(MobEffectCategory.HARMFUL, -12708330) {
    const val TAG_ATTACKER = "BurnAttacker"

    override fun applyEffectTick(entity: LivingEntity, amplifier: Int) {
        val attacker = if (!entity.persistentData.contains(TAG_ATTACKER)) {
            null
        } else {
            entity.level().getEntity(entity.persistentData.getInt(TAG_ATTACKER))
        }

        entity.forceHurt(
            ModDamageTypes.causeBurnDamage(entity.level().registryAccess(), attacker),
            0.6f + (0.3f * amplifier)
        )
        entity.invulnerableTime = 0

        val level = attacker?.level() ?: return
        val player = attacker as? ServerPlayer ?: return
        if (level is ServerLevel) {
            level.playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1f, 1f)
            player.sendPacket(ClientIndicatorMessage(0, 5))
        }
    }

    override fun isDurationEffectTick(pDuration: Int, pAmplifier: Int): Boolean {
        return pDuration % 20 == 0
    }

    @SubscribeEvent
    fun onEffectAdded(event: MobEffectEvent.Added) {
        val living = event.entity
        val instance = event.effectInstance
        if (!instance.effect.equals(ModMobEffects.BURN.get())) {
            return
        }

        living.forceHurt(
            DamageSource(
                living.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.IN_FIRE),
                event.effectSource
            ), 0.6f + (0.3f * instance.amplifier)
        )
        living.invulnerableTime = 0

        val source = event.effectSource
        if (source is LivingEntity) {
            living.persistentData.putInt(TAG_ATTACKER, source.id)
        }
    }

    @SubscribeEvent
    fun onEffectExpired(event: MobEffectEvent.Expired) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.equals(ModMobEffects.BURN.get())) {
            living.persistentData.remove(TAG_ATTACKER)
        }
    }

    @SubscribeEvent
    fun onEffectRemoved(event: MobEffectEvent.Remove) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.equals(ModMobEffects.BURN.get())) {
            living.persistentData.remove(TAG_ATTACKER)
        }
    }

    @SubscribeEvent
    fun onLivingTick(event: LivingEvent.LivingTickEvent) {
        val living = event.entity
        if (living.hasEffect(ModMobEffects.BURN.get())) {
            living.remainingFireTicks = 2
        }
        if (living.isInWater) {
            living.removeEffect(ModMobEffects.BURN.get())
        }
    }
}