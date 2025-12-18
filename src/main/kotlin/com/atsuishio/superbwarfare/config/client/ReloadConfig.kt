package com.atsuishio.superbwarfare.config.client

import com.atsuishio.superbwarfare.config.buildClientConfig

object ReloadConfig {

    @JvmField
    val LEFT_CLICK_RELOAD = buildClientConfig {
        push("reload")

        comment("Set true if you want to reload guns when ammo is empty by clicking left button")
        define("left_click_reload", true).also { pop() }
    }

}
