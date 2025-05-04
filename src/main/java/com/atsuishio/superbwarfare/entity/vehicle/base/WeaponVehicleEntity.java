package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 拥有任意武器的载具
 */
public interface WeaponVehicleEntity extends ArmedVehicleEntity {
    /**
     * 检测该槽位是否有可用武器
     *
     * @param index 武器槽位
     * @return 武器是否可用
     */
    default boolean hasWeapon(int index) {
        if (!(this instanceof VehicleEntity vehicle)) return false;
        if (index < 0 || index >= vehicle.getMaxPassengers()) return false;

        var weapons = getAvailableWeapons(index);
        return !weapons.isEmpty();
    }

    /**
     * 切换武器事件
     *
     * @param index    武器槽位
     * @param value    数值（可能为-1~1之间的滚动，或绝对数值）
     * @param isScroll 是否是滚动事件
     */
    default void changeWeapon(int index, int value, boolean isScroll) {
        if (!(this instanceof VehicleEntity vehicle)) return;
        if (index < 0 || index >= vehicle.getMaxPassengers()) return;

        var weapons = getAvailableWeapons(index);
        if (weapons.isEmpty()) return;
        var count = weapons.size();

        var typeIndex = isScroll ? (value + getWeaponIndex(index) + count) % count : value;
        var weapon = weapons.get(typeIndex);

        // 修改该槽位选择的武器
        setWeaponIndex(index, typeIndex);

        // 播放武器切换音效
        var sound = weapon.sound;
        if (sound != null) {
            vehicle.level().playSound(null, vehicle, sound, vehicle.getSoundSource(), 1, 1);
        }
    }

    /**
     * 获取所有可用武器列表
     */
    default VehicleWeapon[][] getAllWeapons() {
        if (!(this instanceof VehicleEntity vehicle)) return new VehicleWeapon[][]{};

        if (vehicle.availableWeapons == null) {
            vehicle.availableWeapons = new VehicleWeapon[vehicle.getMaxPassengers()][];

            var weapons = this.initWeapons();
            for (int i = 0; i < weapons.length && i < vehicle.getMaxPassengers(); i++) {
                vehicle.availableWeapons[i] = weapons[i];
            }
        }

        return vehicle.availableWeapons;
    }

    /**
     * 初始化所有可用武器列表
     */
    VehicleWeapon[][] initWeapons();

    /**
     * 获取该槽位可用的武器列表
     *
     * @param index 武器槽位
     */
    default List<VehicleWeapon> getAvailableWeapons(int index) {
        if (!(this instanceof VehicleEntity vehicle)) return List.of();
        if (index < 0 || index >= vehicle.getMaxPassengers()) return List.of();

        if (vehicle.availableWeapons[index] != null) {
            return List.of(vehicle.availableWeapons[index]);
        }
        return List.of();
    }

    default VehicleWeapon[][] initAvailableWeapons() {
        if (!(this instanceof VehicleEntity vehicle)) return null;
        if (vehicle.availableWeapons == null) {
            vehicle.availableWeapons = new VehicleWeapon[vehicle.getMaxPassengers()][];
        }

        return vehicle.availableWeapons;
    }

    /**
     * 获取该槽位当前的武器
     *
     * @param index 武器槽位
     */
    default VehicleWeapon getWeapon(int index) {
        if (!(this instanceof VehicleEntity vehicle)) return null;
        if (index < 0 || index >= vehicle.getMaxPassengers()) return null;

        var weapons = getAvailableWeapons(index);
        if (weapons.isEmpty()) return null;

        var type = getWeaponIndex(index);
        if (type < 0 || type >= weapons.size()) return null;

        return weapons.get(type);
    }

    /**
     * 获取该槽位当前的武器编号，返回-1则表示该位置没有可用武器
     *
     * @param index 武器槽位
     * @return 武器类型
     */
    default int getWeaponIndex(int index) {
        if (!(this instanceof VehicleEntity vehicle)) return -1;

        var selectedWeapons = vehicle.getEntityData().get(VehicleEntity.SELECTED_WEAPON);
        if (selectedWeapons.size() <= index) return -1;

        return selectedWeapons.get(index);
    }

    /**
     * 设置该槽位当前的武器编号
     *
     * @param index 武器槽位
     * @param type  武器类型
     */
    default void setWeaponIndex(int index, int type) {
        if (!(this instanceof VehicleEntity vehicle)) return;

        var selectedWeapons = new ArrayList<>(vehicle.getEntityData().get(VehicleEntity.SELECTED_WEAPON));
        selectedWeapons.set(index, type);
        vehicle.getEntityData().set(VehicleEntity.SELECTED_WEAPON, selectedWeapons);
    }

    default void playShootSound3p(Player player, int seat, int radius, int radius2, int radius3) {
        var weapons = getAvailableWeapons(seat);
        var weapon = weapons.get(getWeaponIndex(seat));
        float pitch = getWeaponHeat(player) <= 60 ? 1 : (float) (1 - 0.011 * java.lang.Math.abs(60 - getWeaponHeat(player)));

        if (player instanceof ServerPlayer serverPlayer) {
            if (weapon.sound3p != null) {
                serverPlayer.playSound(weapon.sound3p, radius, pitch);
            }
            if (weapon.sound3pFar != null) {
                serverPlayer.playSound(weapon.sound3pFar, radius2, pitch);
            }
            if (weapon.sound3pVeryFar != null) {
                serverPlayer.playSound(weapon.sound3pVeryFar, radius3, pitch);
            }
        }
    }
}
