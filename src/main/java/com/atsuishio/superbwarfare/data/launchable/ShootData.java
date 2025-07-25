package com.atsuishio.superbwarfare.data.launchable;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// 开火时的信息
public record ShootData(
        @Nullable UUID shooter,
        double damage,
        double explosionDamage,
        double explosionRadius,
        double spread
) {
}
