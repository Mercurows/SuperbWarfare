package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object SpawnConfig {

    @JvmField
    val SPAWN_SENPAI = buildServerConfig {
        push("spawn")

        comment("Set true to allow Senpai to spawn naturally")
        define("spawn_senpai", false)
    }

    @JvmField
    val SPAWN_MOB_WITH_GUNS = buildServerConfig {
        comment("this feature is under development, DO NOT TURN THIS ON!")
        define("spawn_mob_with_guns", false).also { pop() }
    }

}
