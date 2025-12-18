package com.atsuishio.superbwarfare.config

import com.atsuishio.superbwarfare.config.common.GameplayConfig
import net.neoforged.neoforge.common.ModConfigSpec

val COMMON_CONFIG_BUILDER = ModConfigBuilder()

inline fun <T : ModConfigValue> buildCommonConfig(block: ModConfigBuilder.() -> T): (T & Any) {
    return COMMON_CONFIG_BUILDER.block()!!
}

val COMMON_CONFIG: ModConfigSpec = buildConfig(
    COMMON_CONFIG_BUILDER,

    GameplayConfig,
)