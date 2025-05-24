package com.atsuishio.superbwarfare.client.sound;

import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModSoundInstances {

    public static void init() {
        MobileVehicleEntity.trackSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.TrackSound(vehicle));
        MobileVehicleEntity.engineSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.EngineSound(vehicle));
        MobileVehicleEntity.swimSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.SwimSound(vehicle));

        A10Entity.fireSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleFireSoundInstance.A10FireSound(vehicle));
        Hpj11Entity.fireSound = vehicle -> Minecraft.getInstance().getSoundManager().play(new VehicleFireSoundInstance.HPJ11CloseFireSound(vehicle));

        FastThrowableProjectile.flySound = entity -> Minecraft.getInstance().getSoundManager().play(new FastProjectileSoundInstance.FlySound(entity));
        FastThrowableProjectile.nearFlySound = entity -> Minecraft.getInstance().getSoundManager().play(new FastProjectileSoundInstance.NearFlySound(entity));
    }
}
