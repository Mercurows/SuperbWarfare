package com.atsuishio.superbwarfare.api.event;

import com.atsuishio.superbwarfare.data.gun.GunData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@ApiStatus.AvailableSince("0.8.0")
public class ReloadEvent extends Event {

    public final Player player;
    public final GunData data;
    public final ItemStack stack;

    private ReloadEvent(Player player, GunData data) {
        this.player = player;
        this.data = data;
        this.stack = data.stack();
    }

    public static class Pre extends ReloadEvent {
        public Pre(Player player, GunData data) {
            super(player, data);
        }
    }

    public static class Post extends ReloadEvent {
        public Post(Player player, GunData data) {
            super(player, data);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getStack() {
        return stack;
    }
}
