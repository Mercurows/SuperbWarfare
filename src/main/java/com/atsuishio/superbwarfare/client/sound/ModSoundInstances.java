package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModSoundInstances {

    public static void init() {
        VehicleEntity.playTrackSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.TrackSound(vehicle));
        VehicleEntity.playEngineSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.EngineSound(vehicle));
        VehicleEntity.playSwimSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.SwimSound(vehicle));
        VehicleEntity.playHornSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new HornSoundInstance.VehicleHornSound(vehicle));
//        VehicleEntity.playInCarMusic = vehicle -> {
//            if (NetMusicCompatHolder.canPlayMusic(vehicle)) {
//                NetMusicCompatHolder.playMusic(vehicle);
//            } else {
//                Minecraft.getInstance().getSoundManager().play(new InCarMusicInstance.InCarMusicSound(vehicle));
//            }
//        };

        VehicleEntity.playFireSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleFireSoundInstance.VehicleFireSound(vehicle));

        FastThrowableProjectile.playFlySound = entity -> Minecraft.getInstance().getSoundManager().play(new FastProjectileSoundInstance.FlySound(entity));
    }
}
