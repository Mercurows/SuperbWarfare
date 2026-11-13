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
    val DECOY_GRENADE_RADIUS = buildServerConfig {
        push("DecoyGrenade")
        comment("Mob attraction radius in blocks")
        comment("诱饵弹吸引生物的半径（格）")
        defineInRange("radius", 32, 1, 256)
    }

    @JvmField
    val DECOY_GRENADE_DURATION = buildServerConfig {
        comment("Gunshot playback duration in ticks")
        comment("诱饵弹播放枪声的持续时间（tick）")
        defineInRange("duration", 300, 1, Int.MAX_VALUE)
    }

    @JvmField
    val DECOY_GRENADE_LIFETIME = buildServerConfig {
        comment("Maximum total lifetime in ticks")
        comment("诱饵弹总寿命上限（tick）")
        defineInRange("lifetime", 1200, 1, Int.MAX_VALUE)
    }

    @JvmField
    val DECOY_GRENADE_ACTIVATION_DELAY = buildServerConfig {
        comment("Delay after settling in ticks")
        comment("诱饵弹停稳后开始播放枪声的延迟（tick）")
        defineInRange("activation_delay", 40, 0, Int.MAX_VALUE)
    }

    @JvmField
    val DECOY_GRENADE_STOP_SPEED = buildServerConfig {
        comment("Grounded settling speed threshold")
        comment("诱饵弹接触地面后固定位置的速度阈值")
        defineInRange("stop_speed", 0.05, 0.001, 1.0)
    }

    @JvmField
    val DECOY_GRENADE_HIT_DAMAGE = buildServerConfig {
        comment("Direct impact damage")
        comment("诱饵弹投掷命中伤害")
        defineInRange("hit_damage", 1.0, 0.0, 10000000.0)
    }

    @JvmField
    val DECOY_GRENADE_EXPLOSION_DAMAGE = buildServerConfig {
        comment("Explosion damage after playback")
        comment("诱饵弹播放结束后的爆炸伤害")
        defineInRange("explosion_damage", 1.0, 0.0, 10000000.0)
    }

    @JvmField
    val DECOY_GRENADE_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from decoy grenade")
        comment("诱饵弹：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val DECOY_GRENADE_EXPLOSION_RADIUS = buildServerConfig {
        comment("Explosion radius in blocks")
        comment("诱饵弹爆炸半径（格）")
        defineInRange("explosion_radius", 3.0, 0.0, 256.0)
    }

    @JvmField
    val DECOY_GRENADE_ATTRACT_MOBS = buildServerConfig {
        comment("Whether decoy grenades attract mobs")
        comment("诱饵弹是否吸引生物")
        define("attract_mobs", true).also { pop() }
    }

    @JvmField
    val MOLOTOV_FIRE_RADIUS = buildServerConfig {
        push("Molotov")
        comment("Molotov ground fire and burn cloud radius in blocks; set to 0 to disable both")
        comment("燃烧瓶地面火焰与灼烧云的半径（格），设为0关闭两者")
        defineInRange("fire_radius", 3.0, 0.0, 32.0)
    }

    @JvmField
    val MOLOTOV_CLOUD_DURATION = buildServerConfig {
        comment("Molotov burn cloud duration in ticks; set to 0 to disable the cloud")
        comment("燃烧瓶灼烧云持续时间（tick），设为0关闭灼烧云")
        defineInRange("cloud_duration", 200, 0, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val PROJECTILE_CHUNK_AGE = buildServerConfig {
        comment("Ticks before a force-loaded projectile chunk expires without entity activity.")
        comment("投射物强制加载的区块在无实体活动后的存活tick数")
        define("projectile_chunk_age", 20)
            .also { pop() }
    }
}
