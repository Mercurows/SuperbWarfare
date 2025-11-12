package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;

public class Mle1934Model extends VehicleModel<Mle1934Entity> {
    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }
}
