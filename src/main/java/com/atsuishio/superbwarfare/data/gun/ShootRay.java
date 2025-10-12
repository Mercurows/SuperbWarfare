package com.atsuishio.superbwarfare.data.gun;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 开火位置
 *
 * @param shootPosition  子弹位置
 * @param shootDirection 射击方向
 */
public record ShootRay(
        @NotNull Vec3 shootPosition,
        @NotNull Vec3 shootDirection
) {
}
