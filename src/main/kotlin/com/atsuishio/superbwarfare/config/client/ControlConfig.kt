package com.atsuishio.superbwarfare.config.client

import com.atsuishio.superbwarfare.config.buildClientConfig

object ControlConfig {

    @JvmField
    val INVERT_AIRCRAFT_CONTROL = buildClientConfig {
        push("control")

        comment("Set true to invert aircraft control")
        comment("是否反转飞行器鼠标操控")
        define("invert_aircraft_control", false)
    }

    @JvmField
    val MOUSE_SENSITIVITY = buildClientConfig {
        comment("Sensitivity of mouse")
        comment("鼠标灵敏度")
        defineInRange("mouse_sensitivity", 100, 10, 200).also { pop() }
    }
}
