package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;

public class Hpj11Model extends VehicleModel<Hpj11Entity> {

    @Override
    public boolean hideFor1stPassengerWhileZooming() {
        return true;
    }
}
