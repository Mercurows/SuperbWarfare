package com.atsuishio.superbwarfare.config.server;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SpawnConfig {

    public static ModConfigSpec.BooleanValue SPAWN_SENPAI;
    public static ModConfigSpec.BooleanValue SPAWN_MOB_WITH_GUNS;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("spawn");

        builder.comment("Set true to allow Senpai to spawn naturally");
        SPAWN_SENPAI = builder.define("spawn_senpai", false);

        builder.comment("this feature is under development, DO NOT TURN THIS ON!");
        SPAWN_MOB_WITH_GUNS = builder.define("spawn_mob_with_guns", false);

        builder.pop();
    }

}
