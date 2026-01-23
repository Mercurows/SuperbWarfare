package com.atsuishio.superbwarfare.api.event

import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent
import org.jetbrains.annotations.ApiStatus

/**
 * 玩家击杀生物后，用于判断是否发送击杀播报/显示击杀指示
 */
@ApiStatus.AvailableSince("0.8.0")
open class PreKillEvent private constructor(
    val entity: LivingEntity,
    val source: DamageSource,
    val target: LivingEntity
) : Event(), ICancellableEvent {
    class SendKillMessage(entity: LivingEntity, source: DamageSource, target: LivingEntity) :
        PreKillEvent(entity, source, target)

    class Indicator(entity: LivingEntity, source: DamageSource, target: LivingEntity) :
        PreKillEvent(entity, source, target)
}
