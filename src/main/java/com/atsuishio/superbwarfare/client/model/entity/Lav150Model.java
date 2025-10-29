package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Lav150Entity;

public class Lav150Model extends VehicleModel<Lav150Entity> {

    @Override
    public boolean hideFor1stPassengerWhileZooming() {
        return true;
    }

    @Override
    public boolean hasWheel() {
        return true;
    }
}
