package com.atsuishio.superbwarfare.config

import com.atsuishio.superbwarfare.config.client.*
import net.neoforged.neoforge.common.ModConfigSpec

val CLIENT_CONFIG_BUILDER = ModConfigBuilder()

inline fun <T : ModConfigValue> buildClientConfig(block: ModConfigBuilder.() -> T): (T & Any) {
    return CLIENT_CONFIG_BUILDER.block()!!
}

val CLIENT_CONFIG: ModConfigSpec = buildConfig(
    CLIENT_CONFIG_BUILDER,

    ReloadConfig,
    KillMessageConfig,
    DisplayConfig,
    ControlConfig,
    EnvironmentChecksumConfig,
)