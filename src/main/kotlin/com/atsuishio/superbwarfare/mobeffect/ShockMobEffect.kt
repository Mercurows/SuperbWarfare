package com.atsuishio.superbwarfare.mobeffect

import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacket
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraftforge.event.entity.living.LivingAttackEvent
import net.minecraftforge.event.entity.living.LivingEvent
import net.minecraftforge.event.entity.living.MobEffectEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object ShockMobEffect : MobEffect(MobEffectCategory.HARMFUL, -256) {
    // 为什么这里还是 Target
    const val TAG_ATTACKER = "TargetShockAttacker"

    init {
        addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            "7107DE5E-7CE8-4030-940E-514C1F160890",
            -10.0,
            AttributeModifier.Operation.ADDITION
        )
    }

    override fun applyEffectTick(entity: LivingEntity, amplifier: Int) {
        val attacker = if (!entity.persistentData.contains(TAG_ATTACKER)) {
            null
        } else {
            entity.level().getEntity(entity.persistentData.getInt(TAG_ATTACKER))
        }

        entity.forceHurt(
            ModDamageTypes.causeShockDamage(entity.level().registryAccess(), attacker),
            2 + (1.25f * amplifier)
        )
        entity.level().playSound(null, entity.onPos, ModSounds.ELECTRIC.get(), SoundSource.PLAYERS, 1f, 1f)

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
        if (!instance.effect.equals(ModMobEffects.SHOCK.get())) {
            return
        }

        if (living is Player) {
            if (!living.level().isClientSide()) {
                living.level().playSound(
                    null,
                    BlockPos.containing(living.x, living.y, living.z),
                    ModSounds.SHOCK.get(),
                    SoundSource.HOSTILE,
                    1f,
                    1f
                )
            } else {
                living.level().playLocalSound(
                    living.x,
                    living.y,
                    living.z,
                    ModSounds.SHOCK.get(),
                    SoundSource.HOSTILE,
                    1f,
                    1f,
                    false
                )
            }
        }

        living.forceHurt(
            ModDamageTypes.causeShockDamage(
                living.level().registryAccess(),
                event.effectSource
            ), 2 + (1.25f * instance.amplifier)
        )

        val source = event.effectSource
        if (source is LivingEntity) {
            living.persistentData.putInt(TAG_ATTACKER, source.id)
        }
    }

    @SubscribeEvent
    fun onEffectExpired(event: MobEffectEvent.Expired) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.equals(ModMobEffects.SHOCK.get())) {
            living.persistentData.remove(TAG_ATTACKER)
        }
    }

    @SubscribeEvent
    fun onEffectRemoved(event: MobEffectEvent.Remove) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.equals(ModMobEffects.SHOCK.get())) {
            living.persistentData.remove(TAG_ATTACKER)
        }
    }

    @SubscribeEvent
    fun onLivingTick(event: LivingEvent.LivingTickEvent) {
        val living = event.entity

        if (living.hasEffect(ModMobEffects.SHOCK.get())) {
            living.xRot = Mth.nextDouble(RandomSource.create(), -23.0, -36.0).toFloat()
            living.xRotO = living.xRot
        }
    }

    @SubscribeEvent
    fun onEntityAttacked(event: LivingAttackEvent) {
        if (event.entity == null) {
            return
        }
        val source = event.source
        val entity = source.directEntity ?: return
        if (entity is LivingEntity && entity.hasEffect(ModMobEffects.SHOCK.get())) {
            event.isCanceled = true
        }
    }
}