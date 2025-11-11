package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;

public class Mk42Model extends VehicleModel<Mk42Entity> {

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }
}
