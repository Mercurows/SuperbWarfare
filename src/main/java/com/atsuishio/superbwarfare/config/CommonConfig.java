package com.atsuishio.superbwarfare.config;

import com.atsuishio.superbwarfare.config.common.GameplayConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class CommonConfig {

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        GameplayConfig.init(builder);

        return builder.build();
    }

}
