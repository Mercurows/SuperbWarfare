package com.atsuishio.superbwarfare.entity.vehicle.base;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public interface ArmedVehicleEntity {

    default VehicleEntity getVehicleEntity() {
        return (VehicleEntity) this;
    }

    /**
     * 判断指定玩家是否是载具驾驶员
     *
     * @param living 玩家
     * @return 是否是驾驶员
     */
    default boolean isDriver(LivingEntity living) {
        if (this instanceof Entity entity) {
            return living == entity.getFirstPassenger();
        }
        return false;
    }

    boolean canShoot(LivingEntity living);

    /**
     * 瞄准时的放大倍率
     *
     * @return 放大倍率
     */
    @Deprecated
    default int zoomFov() {
        return 1;
    }
}
