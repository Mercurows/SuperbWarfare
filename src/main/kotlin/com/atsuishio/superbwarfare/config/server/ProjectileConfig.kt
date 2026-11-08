package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object ProjectileConfig {
    @JvmField
    val PROJECTILE_DESTROY_BLOCKS = buildServerConfig {
        push("projectile")

        comment("Set true to allow projectiles to destroy certain blocks")
        comment("是否允许子弹破坏方块")
        define("allow_projectile_destroy_blocks", false)
    }

    @JvmField
    val PROJECTILE_CHUNK_LOADING = buildServerConfig {
        comment("Set true to allow projectiles to load chunks")
        comment("是否允许投射物加载区块")
        define("projectile_chunk_loading", true)
    }

    @JvmField
    val PROJECTILE_MAX_CHUNKS_FORCE_LOADED = buildServerConfig {
        comment("Maximum chunks force-loaded by projectiles at once. Set to 0 for unlimited.")
        comment("投射物同时强制加载的最大区块数，设为0表示无限制")
        define("max_projectile_chunks_force_loaded", 256)
    }

    @JvmField
    val PROJECTILE_MAX_CHUNKS_LOADED_EACH_TICK = buildServerConfig {
        comment("Maximum new chunks loaded per tick for projectiles.")
        comment("每tick最多为投射物加载的新区块数")
        define("max_projectile_chunks_loaded_each_tick", 32)
    }

    @JvmField
    val FLASHBANG_RADIUS = buildServerConfig {
        push("Flashbang")
        comment("Maximum flashbang effect radius in blocks; hearing effects depend only on distance")
        comment("闪光弹影响半径（格）；耳鸣仅随距离衰减，闪白和震撼仍受方块遮挡影响")
        defineInRange("flashbang_radius", 32, 1, 256)
    }

    @JvmField
    val FLASHBANG_CORE_RADIUS = buildServerConfig {
        comment("Flashbang core explosion damage radius in blocks")
        comment("闪光弹核心爆炸伤害半径（格）")
        defineInRange("flashbang_core_radius", 3.0, 0.0, 256.0)
    }

    @JvmField
    val FLASHBANG_EXPLOSION_DAMAGE = buildServerConfig {
        comment("Damage dealt to living targets in the visible core; set to 0 to disable")
        comment("闪光弹对无遮挡核心范围内生物造成的爆炸伤害，设为0关闭")
        defineInRange("flashbang_explosion_damage", 1.0, 0.0, 10000000.0)
    }

    @JvmField
    val FLASHBANG_HIT_DAMAGE = buildServerConfig {
        comment("Projectile damage on direct impact, once per target per grenade; set to 0 to disable")
        comment("闪光弹直接命中伤害，每枚弹对同一目标最多触发一次，设为0关闭")
        defineInRange("flashbang_hit_damage", 1.0, 0.0, 10000000.0)
    }

    @JvmField
    val FLASHBANG_STUN_PLAYERS = buildServerConfig {
        comment("Set true to also apply the Stunned mob effect to players affected by flashbangs")
        comment("是否也对受到闪光弹影响的玩家施加震撼效果；不影响闪白和耳鸣")
        define("flashbang_stun_players", false).also { pop() }
    }

    @JvmField
    val PROJECTILE_CHUNK_AGE = buildServerConfig {
        comment("Ticks before a force-loaded projectile chunk expires without entity activity.")
        comment("投射物强制加载的区块在无实体活动后的存活tick数")
        define("projectile_chunk_age", 20)
            .also { pop() }
    }
}
