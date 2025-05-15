package com.atsuishio.superbwarfare.data.launchable;

import java.util.UUID;

// 开火时的信息
public record ShootData(
        UUID shooter,
        double damage,
        double explosionDamage,
        double explosionRadius,
        double spread
) {
}
