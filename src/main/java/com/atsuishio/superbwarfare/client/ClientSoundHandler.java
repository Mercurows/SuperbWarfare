package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.LoudlyEntity;
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.TrackEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientSoundHandler {

    public static void playClientSoundInstance(Entity entity) {
        if (entity instanceof LoudlyEntity) {
            Minecraft.getInstance().getSoundManager().play(new LoudlyEntitySoundInstance.EntitySound(entity));
            Minecraft.getInstance().getSoundManager().play(new LoudlyEntitySoundInstance.EntitySoundClose(entity));
        } else {
            Mod.queueClientWork(60, () -> {
                if (entity instanceof MobileVehicleEntity mobileVehicle) {
                    if (mobileVehicle instanceof TrackEntity) {
                        Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.TrackSound(mobileVehicle));
                    }
                    if (mobileVehicle instanceof A10Entity) {
                        Minecraft.getInstance().getSoundManager().play(new VehicleFireSoundInstance.A10FireSound(mobileVehicle));
                    }
                    if (mobileVehicle instanceof Hpj11Entity) {
                        Minecraft.getInstance().getSoundManager().play(new VehicleFireSoundInstance.HPJ11CloseFireSound(mobileVehicle));
                    }
                    Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.EngineSound(mobileVehicle, mobileVehicle.getEngineSound()));
                    Minecraft.getInstance().getSoundManager().play(new VehicleSoundInstance.SwimSound(mobileVehicle));
                }
            });
        }
    }
}
