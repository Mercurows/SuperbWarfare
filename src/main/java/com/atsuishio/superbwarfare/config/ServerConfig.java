package com.atsuishio.superbwarfare.config;

import com.atsuishio.superbwarfare.config.server.*;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        SpawnConfig.init(builder);
        ProjectileConfig.init(builder);
        ExplosionConfig.init(builder);
        VehicleConfig.init(builder);
        MiscConfig.init(builder);
        SeekConfig.init(builder);

        return builder.build();
    }
}
