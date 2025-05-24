package com.atsuishio.superbwarfare.advancement.criteria;

import com.atsuishio.superbwarfare.init.ModCriteriaTriggers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class OttoSprintTrigger extends SimpleCriterionTrigger<OttoSprintTrigger.TriggerInstance> {

    public void trigger(ServerPlayer pPlayer) {
        this.trigger(pPlayer, instance -> true);
    }

    @Override
    public @NotNull Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleInstance {

        public static final Codec<OttoSprintTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                        .forGetter(OttoSprintTrigger.TriggerInstance::player)).apply(instance, TriggerInstance::new));

        public static Criterion<OttoSprintTrigger.TriggerInstance> get() {
            return ModCriteriaTriggers.OTTO_SPRINT.get().createCriterion(new TriggerInstance(Optional.empty()));
        }

        @Override
        public @NotNull Optional<ContextAwarePredicate> player() {
            return this.player;
        }
    }
}
