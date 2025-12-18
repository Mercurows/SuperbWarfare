package com.atsuishio.superbwarfare.config

import net.minecraftforge.common.ForgeConfigSpec

typealias ModConfig = ForgeConfigSpec
typealias ModConfigBuilder = ForgeConfigSpec.Builder
typealias ModConfigValue = ForgeConfigSpec.ConfigValue<*>?

@Suppress("unused")
fun buildConfig(builder: ModConfigBuilder, vararg configs: Any): ModConfig {
    return builder.build()
}