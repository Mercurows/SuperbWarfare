package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.compat.netmusic.NetMusicCompatHolder;
import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModSoundInstances {

    public static void init() {
        VehicleEntity.trackSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.TrackSound(vehicle));
        VehicleEntity.engineSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.EngineSound(vehicle));
        VehicleEntity.swimSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.SwimSound(vehicle));
        VehicleEntity.hornSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new HornSoundInstance.VehicleHornSound(vehicle));
        VehicleEntity.inCarMusic = vehicle -> {
            if (NetMusicCompatHolder.canPlayMusic(vehicle)) {
                NetMusicCompatHolder.playMusic(vehicle);
            } else {
                Minecraft.getInstance().getSoundManager().play(new InCarMusicInstance.InCarMusicSound(vehicle));
            }
        };

        VehicleEntity.fireSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleFireSoundInstance.VehicleFireSound(vehicle));

        FastThrowableProjectile.flySound = entity -> Minecraft.getInstance().getSoundManager().play(new FastProjectileSoundInstance.FlySound(entity));
        FastThrowableProjectile.nearFlySound = entity -> Minecraft.getInstance().getSoundManager().play(new FastProjectileSoundInstance.NearFlySound(entity));
    }
}
