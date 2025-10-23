package com.atsuishio.superbwarfare.entity.vehicle.base;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public interface ArmedVehicleEntity {

    default VehicleEntity getVehicleEntity() {
        return (VehicleEntity) this;
    }

    /**
     * 载具开火
     *
     * @param living 生物
     */
    void vehicleShoot(LivingEntity living, int type);

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

    /**
     * 主武器射速
     *
     * @return 射速
     */
    int mainGunRpm(LivingEntity living);

    /**
     * 当前情况载具是否可以开火
     *
     * @param living 玩家
     * @return 是否可以开火
     */
    boolean canShoot(LivingEntity living);

    /**
     * 获取当前选择的主武器的备弹数量
     *
     * @param living 玩家
     * @return 备弹数量
     */
    int getAmmoCount(LivingEntity living);

    /**
     * 瞄准时的放大倍率
     *
     * @return 放大倍率
     */
    int zoomFov();

    int getWeaponHeat(LivingEntity living);
}
