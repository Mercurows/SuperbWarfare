package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object ProjectileConfig {

    // TODO 是否考虑重命名该属性？
    @JvmField
    val BLOCK_DESTROY = buildServerConfig {
        push("projectile")

        comment("Set true to allow projectiles to destroy certain blocks")
        define("allow_projectile_destroy_blocks", false)
            .also { pop() }
    }

}
