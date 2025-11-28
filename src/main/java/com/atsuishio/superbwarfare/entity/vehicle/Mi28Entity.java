package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Mi28Entity extends GeoVehicleEntity {

    public Mi28Entity(EntityType<Mi28Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public double getMouseSensitivity() {
        return 0.25;
    }
}
