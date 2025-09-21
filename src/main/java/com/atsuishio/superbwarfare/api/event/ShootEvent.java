package com.atsuishio.superbwarfare.api.event;

import com.atsuishio.superbwarfare.data.gun.GunData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.AvailableSince("0.8.8")
public class ShootEvent extends Event {

    @Nullable
    private final Entity shooter;
    private final ServerLevel level;
    private final GunData data;
    private final double spread;
    private final boolean zoom;

    private ShootEvent(@Nullable Entity shooter, ServerLevel level, GunData data, double spread, boolean zoom) {
        this.shooter = shooter;
        this.level = level;
        this.data = data;
        this.spread = spread;
        this.zoom = zoom;
    }

    public static class Pre extends ShootEvent {

        public Pre(@Nullable Entity shooter, ServerLevel level, GunData data, double spread, boolean zoom) {
            super(shooter, level, data, spread, zoom);
        }
    }

    public static class Post extends ShootEvent {

        public Post(@Nullable Entity shooter, ServerLevel level, GunData data, double spread, boolean zoom) {
            super(shooter, level, data, spread, zoom);
        }
    }

    public @Nullable Entity getShooter() {
        return shooter;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public GunData getData() {
        return data;
    }

    public double getSpread() {
        return spread;
    }

    public boolean isZoom() {
        return zoom;
    }
}
