package com.atsuishio.superbwarfare.config.server;

import net.minecraftforge.common.ForgeConfigSpec;

public class ProjectileConfig {

    public static ForgeConfigSpec.BooleanValue ALLOW_PROJECTILE_DESTROY_BLOCKS;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("projectile");

        builder.comment("Set true to allow projectiles to destroy certain blocks");
        ALLOW_PROJECTILE_DESTROY_BLOCKS = builder.define("allow_projectile_destroy_blocks", false);

        builder.pop();
    }
}
