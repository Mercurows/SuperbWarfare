package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object SpawnConfig {

    @JvmField
    val SPAWN_SENPAI = buildServerConfig {
        push("spawn")

        comment("Set true to allow Senpai to spawn naturally")
        comment("是否允许野兽先辈自然生成（喜）")
        define("spawn_senpai", false)
    }

    @JvmField
    val SPAWN_STEEL_COIL = buildServerConfig {
        comment("Set true to allow Steel Coil to spawn naturally")
        comment("是否允许钢卷自然生成")
        define("spawn_steel_coil", false)
    }

    @JvmField
    val SPAWN_MOB_WITH_GUNS = buildServerConfig {
        comment("this feature is under development, DO NOT TURN THIS ON!")
        comment("是否允许生成持枪生物，测试功能，不要开启")
        define("spawn_mob_with_guns", false).also { pop() }
    }
}
