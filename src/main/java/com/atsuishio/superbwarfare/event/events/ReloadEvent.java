package com.atsuishio.superbwarfare.event.events;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class ReloadEvent extends Event implements ICancellableEvent {

    public final Player player;
    public final ItemStack stack;

    private ReloadEvent(Player player, ItemStack stack) {
        this.player = player;
        this.stack = stack;
    }

    public static class Pre extends ReloadEvent {
        public Pre(Player player, ItemStack stack) {
            super(player, stack);
        }
    }

    public static class Post extends ReloadEvent {
        public Post(Player player, ItemStack stack) {
            super(player, stack);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getStack() {
        return stack;
    }
}
