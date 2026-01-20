package com.atsuishio.superbwarfare.mobeffect

import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.network.NetworkRegistry
import com.atsuishio.superbwarfare.network.message.receive.ClientPhosphorusFireMessage
import com.atsuishio.superbwarfare.tools.DamageHandler
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.entity.living.LivingEvent
import net.minecraftforge.event.entity.living.MobEffectEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.network.PacketDistributor

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object PhosphorusFireMobEffect : MobEffect(MobEffectCategory.HARMFUL, 0xB1C1F2) {
    const val TAG_PHOSPHORUS_FIRE_COUNT = "SbwPhosphorusFireCount"
    const val TAG_PHOSPHORUS_FIRE_ATTACKER = "SbwPhosphorusFireAttacker"

    override fun applyEffectTick(entity: LivingEntity?, amplifier: Int) {
        if (entity == null) return
        val attacker = if (!entity.persistentData.contains(TAG_PHOSPHORUS_FIRE_ATTACKER)) {
            null
        } else {
            entity.level().getEntity(entity.persistentData.getInt(TAG_PHOSPHORUS_FIRE_ATTACKER))
        }

        val fireCount = entity.persistentData.getInt(TAG_PHOSPHORUS_FIRE_COUNT)
        val fireLevel = fireCount / 4

        val damage = 1f + 0.5f * amplifier + ((amplifier + 1) * 5f).coerceAtMost(fireLevel * (amplifier * 0.6f + 1.2f))

        DamageHandler.doDamage(
            entity,
            ModDamageTypes.causePhosphorusFireDamage(entity.level().registryAccess(), null, attacker),
            damage
        )
        entity.invulnerableTime = 0
        entity.persistentData.putInt(TAG_PHOSPHORUS_FIRE_COUNT, fireCount + 1)
    }

    override fun isDurationEffectTick(pDuration: Int, pAmplifier: Int): Boolean {
        return pDuration % 10 == 0
    }

    override fun getCurativeItems(): List<ItemStack> {
        return listOf()
    }

    @SubscribeEvent
    fun onEffectAdded(event: MobEffectEvent.Added) {
        val living = event.entity
        val instance = event.effectInstance

        if (!instance.effect.equals(ModMobEffects.PHOSPHORUS_FIRE.get())) {
            return
        }

        val source = event.effectSource
        if (source is LivingEntity) {
            living.persistentData.putInt(TAG_PHOSPHORUS_FIRE_ATTACKER, source.id)
        }

        NetworkRegistry.PACKET_HANDLER.send(
            PacketDistributor.TRACKING_ENTITY.with { living },
            ClientPhosphorusFireMessage(living.id, true)
        )
    }

    @SubscribeEvent
    fun onEffectExpired(event: MobEffectEvent.Expired) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.equals(ModMobEffects.PHOSPHORUS_FIRE.get())) {
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_ATTACKER)
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_COUNT)

            NetworkRegistry.PACKET_HANDLER.send(
                PacketDistributor.TRACKING_ENTITY.with { living },
                ClientPhosphorusFireMessage(living.id, false)
            )
        }
    }

    @SubscribeEvent
    fun onEffectRemoved(event: MobEffectEvent.Remove) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.equals(ModMobEffects.PHOSPHORUS_FIRE.get())) {
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_ATTACKER)
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_COUNT)

            NetworkRegistry.PACKET_HANDLER.send(
                PacketDistributor.TRACKING_ENTITY.with { living },
                ClientPhosphorusFireMessage(living.id, false)
            )
        }
    }

    @SubscribeEvent
    fun onStartTracking(event: PlayerEvent.StartTracking) {
        val target = event.target
        if (target is LivingEntity) {
            if (target.hasEffect(ModMobEffects.PHOSPHORUS_FIRE.get())) {
                NetworkRegistry.PACKET_HANDLER.send(
                    PacketDistributor.TRACKING_ENTITY.with(event::getEntity),
                    ClientPhosphorusFireMessage(
                        target.id,
                        true
                    )
                )
            }
        }
    }

    @SubscribeEvent
    fun onLivingTick(event: LivingEvent.LivingTickEvent) {
        val living = event.entity
        if (!living.level().isClientSide && living.hasEffect(ModMobEffects.PHOSPHORUS_FIRE.get()) && living.level().gameTime % 1000 == 0.toLong()) {
            NetworkRegistry.PACKET_HANDLER.send(
                PacketDistributor.TRACKING_ENTITY.with(event::getEntity),
                ClientPhosphorusFireMessage(
                    living.id,
                    true
                )
            )
        }
    }
}