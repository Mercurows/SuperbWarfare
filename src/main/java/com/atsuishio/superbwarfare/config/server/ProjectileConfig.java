package com.atsuishio.superbwarfare.config.server;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ProjectileConfig {

    public static ModConfigSpec.BooleanValue ALLOW_PROJECTILE_DESTROY_BLOCKS;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("projectile");

        builder.comment("Set true to allow projectiles to destroy certain blocks");
        ALLOW_PROJECTILE_DESTROY_BLOCKS = builder.define("allow_projectile_destroy_blocks", false);

        builder.pop();
    }
}
