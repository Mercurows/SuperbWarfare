package com.atsuishio.superbwarfare.api.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * 玩家击杀生物后，用于判断是否发送击杀播报/显示击杀指示
 */
@ApiStatus.Internal
@ApiStatus.AvailableSince("0.8.0")
public class PreKillEvent extends Event implements ICancellableEvent {

    private final LivingEntity entity;
    private final DamageSource source;
    private final LivingEntity target;

    private PreKillEvent(LivingEntity entity, DamageSource source, LivingEntity target) {
        this.entity = entity;
        this.source = source;
        this.target = target;
    }

    public static class SendKillMessage extends PreKillEvent {

        public SendKillMessage(LivingEntity player, DamageSource source, LivingEntity target) {
            super(player, source, target);
        }
    }

    public static class Indicator extends PreKillEvent {

        public Indicator(LivingEntity player, DamageSource source, LivingEntity target) {
            super(player, source, target);
        }
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public DamageSource getSource() {
        return source;
    }

    public LivingEntity getTarget() {
        return target;
    }
}
