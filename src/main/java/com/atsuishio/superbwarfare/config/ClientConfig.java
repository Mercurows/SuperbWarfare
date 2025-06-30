package com.atsuishio.superbwarfare.config;

import com.atsuishio.superbwarfare.config.client.*;
import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        ReloadConfig.init(builder);
        KillMessageConfig.init(builder);
        DisplayConfig.init(builder);
        ControlConfig.init(builder);
        EnvironmentChecksumConfig.init(builder);

        return builder.build();
    }
}
