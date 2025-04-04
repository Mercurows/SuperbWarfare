package com.atsuishio.superbwarfare.advancement.criteria;

import com.atsuishio.superbwarfare.init.ModCriteriaTriggers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class RPGMeleeExplosionTrigger extends SimpleCriterionTrigger<RPGMeleeExplosionTrigger.TriggerInstance> {

    public void trigger(ServerPlayer pPlayer) {
        this.trigger(pPlayer, instance -> true);
    }

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public record TriggerInstance(
            Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<RPGMeleeExplosionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                        .forGetter(RPGMeleeExplosionTrigger.TriggerInstance::player)).apply(instance, TriggerInstance::new));

        public static Criterion<RPGMeleeExplosionTrigger.TriggerInstance> get() {
            return ModCriteriaTriggers.RPG_MELEE_EXPLOSION.get().createCriterion(new TriggerInstance(Optional.empty()));
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return this.player;
        }
    }
}
