package com.atsuishio.superbwarfare.config.common

import com.atsuishio.superbwarfare.config.buildCommonConfig

object GameplayConfig {

    @JvmField
    val RESPAWN_RELOAD = buildCommonConfig {
        push("gameplay")

        comment("Set true if you want to reload all your guns when respawn")
        comment("是否开启重生时自动装填弹药")
        define("respawn_reload", true)
    }

    @JvmField
    val GLOBAL_INDICATION = buildCommonConfig {
        comment("Set false if you want to show kill indication ONLY while killing an entity with a gun")
        comment("是否仅在使用枪械击杀生物时触发击杀提示特效")
        define("global_indication", true)
    }

    @JvmField
    val RESPAWN_AUTO_ARMOR = buildCommonConfig {
        comment("Set true if you want to refill your armor plate when respawn")
        comment("是否开启重生时自动使用防弹插板")
        define("respawn_auto_armor", true).also { pop() }
    }

}
