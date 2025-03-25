package com.atsuishio.superbwarfare.config.server;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SpawnConfig {

    public static ModConfigSpec.BooleanValue SPAWN_SENPAI;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("spawn");

        builder.comment("Set true to allow Senpai to spawn naturally");
        SPAWN_SENPAI = builder.define("spawn_senpai", false);

        builder.pop();
    }

}
