package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * 拥有任意武器的载具
 */
// TODO 把这个删了，合并至vehicle entity
public interface WeaponVehicleEntity {
    /**
     * 检测该槽位是否有可用武器
     *
     * @param seatIndex 武器槽位
     * @return 武器是否可用
     */
    default boolean hasWeapon(int seatIndex) {
        if (!(this instanceof VehicleEntity vehicle)) return false;
        if (seatIndex < 0 || seatIndex >= vehicle.getMaxPassengers()) return false;

        var weapons = getAvailableWeapons(seatIndex);
        return !weapons.isEmpty();
    }

    /**
     * 切换武器事件
     *
     * @param seatIndex    武器槽位
     * @param value    数值（可能为-1~1之间的滚动，或绝对数值）
     * @param isScroll 是否是滚动事件
     */
    default void changeWeapon(int seatIndex, int value, boolean isScroll) {
        if (!(this instanceof VehicleEntity vehicle)) return;
        if (seatIndex < 0 || seatIndex >= vehicle.getMaxPassengers()) return;

        var weapons = getAvailableWeapons(seatIndex);
        if (weapons.isEmpty()) return;
        var count = weapons.size();

        var typeIndex = Mth.clamp(isScroll ? (value + getWeaponIndex(seatIndex) + count) % count : value, 0, count - 1);
        var weapon = weapons.get(typeIndex);

        // 修改该槽位选择的武器
        setWeaponIndex(seatIndex, typeIndex);

        // 播放武器切换音效
        var sound = weapon.sound;
        if (sound != null) {
            vehicle.level().playSound(null, vehicle, sound, vehicle.getSoundSource(), 1, 1);
        }
    }

    /**
     * 获取该槽位可用的武器列表
     *
     * @param seatIndex 武器槽位
     */
    default List<VehicleWeapon> getAvailableWeapons(int seatIndex) {
        if (!(this instanceof VehicleEntity vehicle)) return List.of();
        if (seatIndex < 0 || seatIndex >= vehicle.getMaxPassengers()) return List.of();

        if (vehicle.availableWeapons != null && vehicle.availableWeapons[seatIndex] != null) {
            return List.of(vehicle.availableWeapons[seatIndex]);
        }
        return List.of();
    }

    /**
     * 获取该槽位当前的武器
     *
     * @param seatIndex 武器槽位
     */
    default VehicleWeapon getWeapon(int seatIndex) {
        if (!(this instanceof VehicleEntity vehicle)) return null;
        if (seatIndex < 0 || seatIndex >= vehicle.getMaxPassengers()) return null;

        var weapons = getAvailableWeapons(seatIndex);
        if (weapons.isEmpty()) return null;

        var type = getWeaponIndex(seatIndex);
        if (type < 0 || type >= weapons.size()) return null;

        return weapons.get(type);
    }

    /**
     * 获取该槽位当前的武器编号，返回-1则表示该位置没有可用武器
     *
     * @param seatIndex 槽位
     * @return 武器类型
     */
    default int getWeaponIndex(int seatIndex) {
        if (!(this instanceof VehicleEntity vehicle)) return -1;

        var selectedWeapons = vehicle.getEntityData().get(VehicleEntity.SELECTED_WEAPON);
        if (selectedWeapons.size() <= seatIndex) return -1;

        return selectedWeapons.get(seatIndex);
    }

    /**
     * 设置该槽位当前的武器编号
     *
     * @param seatIndex 武器槽位
     * @param selectedWeapon  武器类型
     */
    default void setWeaponIndex(int seatIndex, int selectedWeapon) {
        if (!(this instanceof VehicleEntity vehicle)) return;

        var selectedWeapons = new ArrayList<>(vehicle.getEntityData().get(VehicleEntity.SELECTED_WEAPON));

        var oldIndex = selectedWeapons.get(seatIndex);
        if (oldIndex == selectedWeapon) return;

        vehicle.modifyGunData(seatIndex, oldIndex, gunData -> {
            if (gunData.compute().withdrawAmmoWhenChangeSlot) {
                gunData.withdrawAmmo(vehicle.getAmmoSupplier());
            }
        });

        selectedWeapons.set(seatIndex, selectedWeapon);
        vehicle.getEntityData().set(VehicleEntity.SELECTED_WEAPON, selectedWeapons);
    }
}
