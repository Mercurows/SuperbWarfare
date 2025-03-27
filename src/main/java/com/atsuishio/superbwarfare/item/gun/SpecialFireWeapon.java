package com.atsuishio.superbwarfare.item.gun;

import net.minecraft.world.entity.player.Player;


/**
 * 拥有特殊开火方式的武器的开火处理
 */
public interface SpecialFireWeapon {


    /**
     * 按下按键时武器发射处理
     *
     * @param player 玩家
     */

    default void fireOnPress(Player player) {
    }

    /**
     * 松开按键时武器发射处理
     *
     * @param player 玩家
     */
    default void fireOnRelease(Player player) {
    }

}
