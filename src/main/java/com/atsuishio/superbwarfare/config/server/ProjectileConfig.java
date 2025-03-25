package com.atsuishio.superbwarfare.config.server;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ProjectileConfig {

    public static ModConfigSpec.BooleanValue ALLOW_PROJECTILE_DESTROY_GLASS;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("projectile");

        builder.comment("Set true to allow projectiles to destroy glasses");
        ALLOW_PROJECTILE_DESTROY_GLASS = builder.define("allow_projectile_destroy_glass", false);

        builder.pop();
    }
}
