package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object VehicleConfig {

    @JvmField
    val COLLISION_DESTROY_SOFT_BLOCKS = buildServerConfig {
        push("vehicle")
        push("collision")

        comment("Allows vehicles to destroy soft blocks via collision")
        define("collision_destroy_soft_blocks", false)
    }

    @JvmField
    val COLLISION_DESTROY_NORMAL_BLOCKS = buildServerConfig {
        comment("Allows vehicles to destroy normal blocks via collision")
        define("collision_destroy_normal_blocks", false)
    }

    @JvmField
    val COLLISION_DESTROY_HARD_BLOCKS = buildServerConfig {
        comment("Allows vehicles to destroy hard blocks via collision")
        define("collision_destroy_hard_blocks", false)
    }

    @JvmField
    val COLLISION_DESTROY_BLOCKS_BEASTLY = buildServerConfig {
        comment("Allows vehicles to destroy blocks via collision like a beast")
        define("collision_destroy_blocks_beastly", false)
    }

    @JvmField
    val COLLISION_ENTITY_WHITELIST = buildServerConfig {
        comment("List of entities that can be damaged by collision")
        defineList("collision_entity_whitelist", listOf<String>(), { "" }) { it is String }
            .also { pop() }
    }

    @JvmField
    val VEHICLE_ITEM_PICKUP = buildServerConfig {
        comment("Allow vehicles to pick up items")
        define("vehicle_item_pickup", true)
    }

    @JvmField
    val COLLECT_DROPS_BY_CRASHING = buildServerConfig {
        comment("Allow vehicles to collect drops after killing other entities by crashing")
        define("collect_drops_by_crashing", true)
    }

    @JvmField
    val VEHICLE_INFO_DISPLAY_DISTANCE = buildServerConfig {
        comment("Within this distance, the vehicle info will be displayed at client side")
        defineInRange("vehicle_info_display_distance", 196, 0, 1024)
    }

    @JvmField
    val SELF_EXPLOSION_DAMAGE = buildServerConfig {
        comment("The damage of self explosion when a vehicle is destroyed")
        defineInRange("self_explosion_damage", 114514, 0, Int.MAX_VALUE)
    }

    @JvmField
    val SELF_EXPLOSION_COUNT = buildServerConfig {
        comment("The damage count of self explosion when a vehicle is destroyed")
        defineInRange("self_explosion_count", 5, 0, 100)
    }

    @JvmField
    val AIR_CRASH_EXPLOSION_DAMAGE = buildServerConfig {
        comment("The air crash damage when an aircraft is destroyed")
        defineInRange("air_crash_explosion_damage", 114514, 0, Int.MAX_VALUE)
    }

    @JvmField
    val AIR_CRASH_EXPLOSION_COUNT = buildServerConfig {
        comment("The air crash damage count when an aircraft is destroyed")
        defineInRange("air_crash_explosion_count", 5, 0, 100)
    }

    @JvmField
    val TURRET_WRECKAGE_LOOT_RATE = buildServerConfig {
        comment("The rate of recycling loot items from turret wreckage")
        defineInRange("turret_wreckage_loot_rate", 0.3, 0.0, 1.0)
    }

    @JvmField
    val REPAIR_COOLDOWN = buildServerConfig {
        push("repair")

        comment("The default cooldown of vehicle repair. Set a negative value to disable vehicle repair")
        defineInRange("repair_cooldown", 200, -1, 100000000)
    }

    @JvmField
    val REPAIR_AMOUNT = buildServerConfig {
        comment("The default amount of health restored per tick when a vehicle is self-repairing")
        defineInRange("repair_amount", 0.05, -100000000.0, 100000000.0).also { pop(2) }
    }
}