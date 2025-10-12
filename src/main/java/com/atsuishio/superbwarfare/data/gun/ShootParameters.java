package com.atsuishio.superbwarfare.data.gun;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 开火参数
 *
 * @param ammoSupplier   弹药提供者
 * @param shooter        射击者
 * @param level          ServerLevel
 * @param shootPosition  子弹位置
 * @param shootDirection 射击方向
 * @param data           GunData
 * @param spread         子弹散布
 * @param zoom           是否开镜
 * @param targetEntityUUID           已锁定实体UUID
 */
public record ShootParameters(
        @Nullable Entity ammoSupplier,
        @Nullable Entity shooter,
        @NotNull ServerLevel level,
        @NotNull Vec3 shootPosition,
        @NotNull Vec3 shootDirection,
        @NotNull GunData data,
        double spread,
        boolean zoom,
        @Nullable UUID targetEntityUUID
) {
}
