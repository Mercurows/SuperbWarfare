package com.atsuishio.superbwarfare.mobeffect

import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraftforge.event.entity.living.MobEffectEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object PhosphorusFireMobEffect : MobEffect(MobEffectCategory.HARMFUL, 0xB1C1F2) {
    const val TAG_PHOSPHORUS_FIRE_COUNT = "SbwPhosphorusFireCount"
    const val TAG_PHOSPHORUS_FIRE_ATTACKER = "SbwPhosphorusFireAttacker"

    override fun applyEffectTick(entity: LivingEntity, amplifier: Int) {
        val attacker = if (!entity.persistentData.contains(TAG_PHOSPHORUS_FIRE_ATTACKER)) {
            null
        } else {
            entity.level().getEntity(entity.persistentData.getInt(TAG_PHOSPHORUS_FIRE_ATTACKER))
        }

        val fireCount = entity.persistentData.getInt(TAG_PHOSPHORUS_FIRE_COUNT)
        val fireLevel = fireCount / 4

        var damage = 1f + 0.5f * amplifier + ((amplifier + 1) * 5f).coerceAtMost(fireLevel * (amplifier * 0.6f + 1.2f))
        if (entity.isInWater) {
            damage /= 1.5f
        }
        if (entity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            damage /= 2f
        }

        val fireResLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_PROTECTION, entity)
        damage /= 1 + fireResLevel * 0.1f

        entity.forceHurt(
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

        // 服务端成为唯一数据源，由 CapabilitySync 自动同步给所有跟踪该实体的客户端
        PhosphorusFireCapability.set(living, true)
    }

    @SubscribeEvent
    fun onEffectExpired(event: MobEffectEvent.Expired) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.equals(ModMobEffects.PHOSPHORUS_FIRE.get())) {
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_ATTACKER)
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_COUNT)

            PhosphorusFireCapability.set(living, false)
        }
    }

    @SubscribeEvent
    fun onEffectRemoved(event: MobEffectEvent.Remove) {
        val living = event.entity
        val instance = event.effectInstance ?: return

        if (instance.effect.equals(ModMobEffects.PHOSPHORUS_FIRE.get())) {
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_ATTACKER)
            living.persistentData.remove(TAG_PHOSPHORUS_FIRE_COUNT)

            PhosphorusFireCapability.set(living, false)
        }
    }
}