package com.atsuishio.superbwarfare.advancement.criteria

import com.atsuishio.superbwarfare.Mod
import com.google.gson.JsonObject
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance
import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.DeserializationContext
import net.minecraft.advancements.critereon.SimpleCriterionTrigger
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

class OttoSprintTrigger : SimpleCriterionTrigger<OttoSprintTrigger.TriggerInstance>() {
    companion object {
        val ID = Mod.loc("otto_sprint")
    }

    override fun createInstance(
        pJson: JsonObject,
        pPredicate: ContextAwarePredicate,
        pDeserializationContext: DeserializationContext
    ): TriggerInstance {
        return TriggerInstance(pPredicate)
    }

    override fun getId(): ResourceLocation {
        return ID
    }

    fun trigger(pPlayer: ServerPlayer) {
        this.trigger(pPlayer) { _ -> true }
    }

    class TriggerInstance(player: ContextAwarePredicate) : AbstractCriterionTriggerInstance(ID, player) {
        companion object {
            @JvmStatic
            fun get(): TriggerInstance {
                return TriggerInstance(ContextAwarePredicate.ANY)
            }
        }
    }
}
