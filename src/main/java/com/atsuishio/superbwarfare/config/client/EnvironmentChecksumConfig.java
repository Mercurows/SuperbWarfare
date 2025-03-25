package com.atsuishio.superbwarfare.config.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public class EnvironmentChecksumConfig {

    public static ModConfigSpec.ConfigValue<String> ENVIRONMENT_CHECKSUM;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("checksum");

        builder.comment("System environment checksum, do not edit");
        ENVIRONMENT_CHECKSUM = builder.define("environment_checksum", "");

        builder.pop();
    }

}
