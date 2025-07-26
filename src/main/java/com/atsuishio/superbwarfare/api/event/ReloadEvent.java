package com.atsuishio.superbwarfare.api.event;

import com.atsuishio.superbwarfare.data.gun.GunData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
@ApiStatus.AvailableSince("0.8.0")
public class ReloadEvent extends Event {

    public final Entity shooter;
    public final GunData data;
    public final ItemStack stack;

    private ReloadEvent(Entity shooter, GunData data) {
        this.shooter = shooter;
        this.data = data;
        this.stack = data.stack;
    }

    public static class Pre extends ReloadEvent {
        public Pre(@Nullable Entity shooter, GunData data) {
            super(shooter, data);
        }
    }

    public static class Post extends ReloadEvent {
        public Post(@Nullable Entity shooter, GunData data) {
            super(shooter, data);
        }
    }

    public @Nullable Entity getEntity() {
        return shooter;
    }

    public ItemStack getStack() {
        return stack;
    }
}
