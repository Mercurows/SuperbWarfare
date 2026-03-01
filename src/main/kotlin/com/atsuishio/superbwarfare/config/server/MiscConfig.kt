package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object MiscConfig {
    @JvmField
    val SEND_KILL_FEEDBACK = buildServerConfig {
        push("misc")

        comment("Set true to enable kill feedback sending")
        define("send_kill_feedback", true)
    }

    @JvmField
    val FORCE_DAMAGE_MODE = buildServerConfig {
        comment("Set true to enable force damage")
        define("force_damage_mode", false)
    }

    @JvmField
    val DROP_AMMO_BOX = buildServerConfig {
        comment("Whether to drop an ammo box after the player dies")
        define("drop_ammo_box", false)
    }

    @JvmField
    val DEFAULT_ARMOR_LEVEL = buildServerConfig {
        comment("The default maximum armor level for normal armors")
        defineInRange("default_armor_level", 1, 0, 10000000)
    }

    @JvmField
    val MILITARY_ARMOR_LEVEL = buildServerConfig {
        comment("The maximum armor level for armors with superbwarfare:military_armor tag")
        defineInRange("military_armor_level", 2, 0, 10000000)
    }

    @JvmField
    val HEAVY_MILITARY_ARMOR_LEVEL = buildServerConfig {
        comment("The maximum armor level for armors with superbwarfare:military_armor_heavy tag(will override superbwarfare:military_armor tag!)")
        defineInRange("heavy_military_armor_level", 3, 0, 10000000)
    }

    @JvmField
    val ARMOR_POINT_PER_LEVEL = buildServerConfig {
        comment("The points per level for armor plate")
        defineInRange("armor_point_per_level", 15, 0, 10000000)
    }

    @JvmField
    val CHARGING_STATION_MAX_ENERGY = buildServerConfig {
        comment("Max energy storage of charging station")
        defineInRange("charging_station_max_energy", 4000000, 1, Int.MAX_VALUE)
    }

    @JvmField
    val CHARGING_STATION_GENERATE_SPEED = buildServerConfig {
        comment("How much FE energy can charging station generate per tick")
        defineInRange("charging_station_generate_speed", 128, 1, Int.MAX_VALUE)
    }

    @JvmField
    val CHARGING_STATION_TRANSFER_SPEED = buildServerConfig {
        comment("How much FE energy can charging station transfer per tick")
        defineInRange("charging_station_transfer_speed", 100000, 1, Int.MAX_VALUE)
    }

    @JvmField
    val CHARGING_STATION_CHARGE_RADIUS = buildServerConfig {
        comment("The charging radius of the charging station")
        defineInRange("charging_station_charge_radius", 8, 0, 128)
    }

    @JvmField
    val CHARGING_STATION_DEFAULT_FUEL_TIME = buildServerConfig {
        comment("The default fuel time of the charging station")
        defineInRange("charging_station_default_fuel_time", 1600, 1, Int.MAX_VALUE)
    }

    @JvmField
    val ARTILLERY_INDICATOR_LIST_SIZE = buildServerConfig {
        comment("The max size of artillery indicator binding list")
        defineInRange("artillery_indicator_list_size", 32, 1, Int.MAX_VALUE)
    }

    @JvmField
    val MINE_HITBOX_INVISIBLE = buildServerConfig {
        comment("Set true to make mine hitbox invisible")
        define("mine_hitbox_invisible", false)
    }

    @JvmField
    val BLUEPRINT_RESEARCH_TABLE_MAX_FUEL = buildServerConfig {
        comment("The max fuel count of blueprint research table")
        defineInRange("blueprint_research_table_max_fuel", 8, 1, 128)
    }

    @JvmField
    val SMOKE_HIDE_TARGET = buildServerConfig {
        comment("Set true to allow smoke to prevent entities from being set as target")
        define("smoke_hide_target", false).also { pop() }
    }
}