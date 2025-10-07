package com.atsuishio.superbwarfare.api.event;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.AvailableSince("0.8.8")
public class ShootEvent extends Event {

    private final ShootParameters parameters;

    private ShootEvent(@NotNull ShootParameters parameters) {
        this.parameters = parameters;
    }

    public static class Pre extends ShootEvent {

        public Pre(@NotNull ShootParameters parameters) {
            super(parameters);
        }
    }

    public static class Post extends ShootEvent {

        public Post(@NotNull ShootParameters parameters) {
            super(parameters);
        }
    }

    public @NotNull ShootParameters getShootParameters() {
        return this.parameters;
    }

    public @Nullable Entity getShooter() {
        return parameters.shooter();
    }

    public ServerLevel getLevel() {
        return parameters.level();
    }

    public GunData getData() {
        return parameters.data();
    }

    public double getSpread() {
        return parameters.spread();
    }

    public boolean isZoom() {
        return parameters.zoom();
    }
}
