package com.atsuishio.superbwarfare.config;

import com.atsuishio.superbwarfare.config.client.*;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        ReloadConfig.init(builder);
        KillMessageConfig.init(builder);
        DisplayConfig.init(builder);
        VehicleControlConfig.init(builder);
        EnvironmentChecksumConfig.init(builder);

        return builder.build();
    }
}
