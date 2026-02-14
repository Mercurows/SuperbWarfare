package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object ProjectileConfig {
    @JvmField
    val PROJECTILE_DESTROY_BLOCKS = buildServerConfig {
        push("projectile")

        comment("Set true to allow projectiles to destroy certain blocks")
        define("allow_projectile_destroy_blocks", false)
            .also { pop() }
    }
}
