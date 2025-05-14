package com.atsuishio.superbwarfare.api.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * 玩家击杀生物后，用于判断是否发送击杀播报/显示击杀指示
 */
@ApiStatus.Internal
@ApiStatus.AvailableSince("0.8.0")
public class PreKillEvent extends Event implements ICancellableEvent {

    private final Player player;
    private final DamageSource source;
    private final LivingEntity target;

    private PreKillEvent(Player player, DamageSource source, LivingEntity target) {
        this.player = player;
        this.source = source;
        this.target = target;
    }

    public static class SendKillMessage extends PreKillEvent {

        public SendKillMessage(Player player, DamageSource source, LivingEntity target) {
            super(player, source, target);
        }
    }

    public static class Indicator extends PreKillEvent {

        public Indicator(Player player, DamageSource source, LivingEntity target) {
            super(player, source, target);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public DamageSource getSource() {
        return source;
    }

    public LivingEntity getTarget() {
        return target;
    }
}
