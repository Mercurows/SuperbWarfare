package com.atsuishio.superbwarfare.advancement.criteria

import com.atsuishio.superbwarfare.Mod
import com.google.gson.JsonObject
import net.minecraft.advancements.critereon.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource

class VehicleHurtTrigger : SimpleCriterionTrigger<VehicleHurtTrigger.TriggerInstance>() {
    companion object {
        val ID = Mod.loc("vehicle_hurt")
    }

    override fun createInstance(
        pJson: JsonObject,
        pPredicate: ContextAwarePredicate,
        pDeserializationContext: DeserializationContext
    ): TriggerInstance {
        val damagePredicate = DamagePredicate.fromJson(pJson.get("damage"))
        return TriggerInstance(pPredicate, damagePredicate)
    }

    override fun getId(): ResourceLocation {
        return ID
    }

    fun trigger(pPlayer: ServerPlayer, source: DamageSource, amount: Float) {
        this.trigger(pPlayer) { instance -> instance.matches(pPlayer, source, amount) }
    }

    class TriggerInstance(
        player: ContextAwarePredicate,
        private val damage: DamagePredicate
    ) : AbstractCriterionTriggerInstance(ID, player) {

        companion object {
            @JvmStatic
            fun vehicleHurt(): TriggerInstance {
                return TriggerInstance(ContextAwarePredicate.ANY, DamagePredicate.ANY)
            }

            @JvmStatic
            fun vehicleHurt(damage: DamagePredicate): TriggerInstance {
                return TriggerInstance(ContextAwarePredicate.ANY, damage)
            }

            @JvmStatic
            fun vehicleHurt(damageBuilder: DamagePredicate.Builder): TriggerInstance {
                return TriggerInstance(ContextAwarePredicate.ANY, damageBuilder.build())
            }
        }

        fun matches(pPlayer: ServerPlayer, source: DamageSource, amount: Float): Boolean {
            return this.damage.matches(pPlayer, source, amount, amount, false)
        }

        override fun serializeToJson(pConditions: SerializationContext): JsonObject {
            val jsonObject = super.serializeToJson(pConditions)
            jsonObject.add("damage", this.damage.serializeToJson())
            return jsonObject
        }
    }
}
