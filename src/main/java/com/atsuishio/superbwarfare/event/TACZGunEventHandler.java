package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;

public class TACZGunEventHandler {
    public static void entityHurtByTACZGun(EntityHurtByGunEvent.Pre event) {
        if (event.getHurtEntity() instanceof VehicleEntity) {
            event.setHeadshot(false);
        }
    }
}
