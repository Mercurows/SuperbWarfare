package com.atsuishio.superbwarfare.config;

import com.atsuishio.superbwarfare.config.server.*;
import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {

    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        SpawnConfig.init(builder);
        ProjectileConfig.init(builder);
        ExplosionConfig.init(builder);
        VehicleConfig.init(builder);
        MiscConfig.init(builder);

        return builder.build();
    }
}
