package com.atsuishio.superbwarfare.advancement.criteria

import com.atsuishio.superbwarfare.init.ModCriteriaTriggers
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.advancements.Criterion
import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.SimpleCriterionTrigger
import net.minecraft.server.level.ServerPlayer
import java.util.*

class OttoSprintTrigger : SimpleCriterionTrigger<OttoSprintTrigger.TriggerInstance>() {

    fun trigger(pPlayer: ServerPlayer) {
        this.trigger(pPlayer) { true }
    }

    override fun codec(): Codec<TriggerInstance> {
        return TriggerInstance.CODEC
    }

    @JvmRecord
    data class TriggerInstance(val playerVar: Optional<ContextAwarePredicate>) : SimpleInstance {
        override fun player() = this.playerVar

        companion object {
            val CODEC: Codec<TriggerInstance> = RecordCodecBuilder.create { instance ->
                instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::playerVar)
                ).apply(instance) { player -> TriggerInstance(player) }
            }

            @JvmStatic
            fun get(): Criterion<TriggerInstance> = ModCriteriaTriggers.OTTO_SPRINT.get()
                .createCriterion(TriggerInstance(Optional.empty<ContextAwarePredicate>()))
        }
    }
}
