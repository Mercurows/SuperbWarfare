package com.atsuishio.superbwarfare.config

import com.atsuishio.superbwarfare.config.server.*

val SERVER_CONFIG_BUILDER = ModConfigBuilder()

inline fun <T : ModConfigValue> buildServerConfig(block: ModConfigBuilder.() -> T): (T & Any) {
    return SERVER_CONFIG_BUILDER.block()!!
}

val SERVER_CONFIG = buildConfig(
    SERVER_CONFIG_BUILDER,

    SpawnConfig,
    ProjectileConfig,
    ExplosionConfig,
    VehicleConfig,
    MiscConfig,
    AmmoConfig,
)