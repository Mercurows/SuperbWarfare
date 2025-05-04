package com.atsuishio.superbwarfare.entity.vehicle.base;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface ArmedVehicleEntity {

    /**
     * 载具开火
     *
     * @param player 玩家
     */
    void vehicleShoot(Player player, int type);

    /**
     * 判断指定玩家是否是载具驾驶员
     *
     * @param player 玩家
     * @return 是否是驾驶员
     */
    default boolean isDriver(Player player) {
        if (this instanceof Entity entity) {
            return player == entity.getFirstPassenger();
        }
        return false;
    }

    /**
     * 主武器射速
     *
     * @return 射速
     */
    int mainGunRpm(Player player);

    /**
     * 当前情况载具是否可以开火
     *
     * @param player 玩家
     * @return 是否可以开火
     */
    boolean canShoot(Player player);

    /**
     * 获取当前选择的主武器的备弹数量
     *
     * @param player 玩家
     * @return 备弹数量
     */
    int getAmmoCount(Player player);

    /**
     * 是否禁用玩家手臂
     *
     * @param player 玩家
     */
    default boolean banHand(Player player) {
        // 若玩家所在位置有可用武器，则默认禁用手臂
        if (this instanceof VehicleEntity vehicle && this instanceof WeaponVehicleEntity weaponVehicle) {
            return weaponVehicle.hasWeapon(vehicle.getSeatIndex(player));
        }
        return false;
    }

    /**
     * 是否隐藏载具上的玩家
     *
     * @return 是否隐藏
     */
    boolean hidePassenger(Entity entity);

    /**
     * 瞄准时的放大倍率
     *
     * @return 放大倍率
     */
    int zoomFov();

    int getWeaponHeat(Player player);
}
