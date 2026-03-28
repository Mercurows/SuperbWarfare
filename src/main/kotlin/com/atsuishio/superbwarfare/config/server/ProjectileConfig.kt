package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object ProjectileConfig {
    @JvmField
    val PROJECTILE_DESTROY_BLOCKS = buildServerConfig {
        push("projectile")

        comment("Set true to allow projectiles to destroy certain blocks")
        comment("是否允许子弹破坏方块")
        define("allow_projectile_destroy_blocks", false)
            .also { pop() }
    }
}
