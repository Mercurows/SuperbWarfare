package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object MapConfig {
    @JvmField
    val ENABLE_TACTICAL_MAP = buildServerConfig {
        push("map")

        comment("Set true to enable tactical map (Experimental)")
        comment("是否启用战术地图（测试功能，慎用）")
        define("enable_tactical_map", false).also { pop() }
    }
}