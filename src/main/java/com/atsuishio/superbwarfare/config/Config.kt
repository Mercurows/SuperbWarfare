package com.atsuishio.superbwarfare.config

import net.neoforged.neoforge.common.ModConfigSpec

typealias ModConfig = ModConfigSpec
typealias ModConfigBuilder = ModConfigSpec.Builder
typealias ModConfigValue = ModConfigSpec.ConfigValue<*>?

@Suppress("unused")
fun buildConfig(builder: ModConfigBuilder, vararg configs: Any): ModConfig {
    return builder.build()
}