package com.atsuishio.superbwarfare;

import com.atsuishio.superbwarfare.client.VehicleSoundInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import net.minecraft.client.Minecraft;


public class ModClient {
    public static void init() {
        initEntities();
    }

    public static void initEntities() {
        MobileVehicleEntity.engineSound = mobileVehicle -> {
            var client = Minecraft.getInstance();
            client.getSoundManager().play(new VehicleSoundInstance.EngineSound(client, mobileVehicle));
        };
    }
}
