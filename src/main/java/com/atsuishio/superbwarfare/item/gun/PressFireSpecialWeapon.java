package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.item.gun.data.GunData;
import net.minecraft.world.entity.player.Player;

/**
 * 不使用普通子弹的按下开火武器的开火处理
 */
public interface PressFireSpecialWeapon {

    /**
     * 按下按键时武器发射处理
     *
     * @param player 玩家
     */

    default void fireOnPress(Player player, GunData data, double spread, boolean zoom) {
    }
}
