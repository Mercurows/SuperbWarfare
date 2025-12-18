package com.atsuishio.superbwarfare.config.common

import com.atsuishio.superbwarfare.config.buildCommonConfig

object GameplayConfig {

    @JvmField
    val RESPAWN_RELOAD = buildCommonConfig {
        push("gameplay")

        comment("Set true if you want to reload all your guns when respawn")
        define("respawn_reload", true)
    }

    @JvmField
    val GLOBAL_INDICATION = buildCommonConfig {
        comment("Set false if you want to show kill indication ONLY while killing an entity with a gun")
        define("global_indication", true)
    }

    @JvmField
    val RESPAWN_AUTO_ARMOR = buildCommonConfig {
        comment("Set true if you want to refill your armor plate when respawn")
        define("respawn_auto_armor", true).also { pop() }
    }

}
