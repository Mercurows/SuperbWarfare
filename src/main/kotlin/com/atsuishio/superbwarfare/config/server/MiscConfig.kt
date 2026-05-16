package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object MiscConfig {
    @JvmField
    val SEND_KILL_FEEDBACK = buildServerConfig {
        push("misc")

        comment("Set true to enable kill feedback sending")
        comment("是否在服务端启用击杀提示播报")
        define("send_kill_feedback", true)
    }

    @JvmField
    val FORCE_DAMAGE_MODE = buildServerConfig {
        comment("Set true to enable force damage")
        comment("是否开启强制伤害模式")
        define("force_damage_mode", false)
    }

    @JvmField
    val DROP_AMMO_BOX = buildServerConfig {
        comment("Whether to drop an ammo box after the player dies")
        comment("玩家在死亡时，是否掉落弹药")
        define("drop_ammo_box", false)
    }

    @JvmField
    val DEFAULT_ARMOR_LEVEL = buildServerConfig {
        comment("The default maximum armor level for normal armors")
        comment("普通护甲能加的防弹插板数量")
        defineInRange("default_armor_level", 1, 0, 10000000)
    }

    @JvmField
    val MILITARY_ARMOR_LEVEL = buildServerConfig {
        comment("The maximum armor level for armors with superbwarfare:military_armor tag")
        comment("军事护甲能加的防弹插板数量")
        defineInRange("military_armor_level", 2, 0, 10000000)
    }

    @JvmField
    val HEAVY_MILITARY_ARMOR_LEVEL = buildServerConfig {
        comment("The maximum armor level for armors with superbwarfare:military_armor_heavy tag(will override superbwarfare:military_armor tag!)")
        comment("重型护甲能加的防弹插板数量")
        defineInRange("heavy_military_armor_level", 3, 0, 10000000)
    }

    @JvmField
    val ARMOR_POINT_PER_LEVEL = buildServerConfig {
        comment("The points per level for armor plate")
        comment("防弹插板提供的伤害吸收量")
        defineInRange("armor_point_per_level", 15, 0, 10000000)
    }

    @JvmField
    val CHARGING_STATION_MAX_ENERGY = buildServerConfig {
        comment("Max energy storage of charging station")
        comment("充电站的最大存储能量")
        defineInRange("charging_station_max_energy", 4000000, 1, Int.MAX_VALUE)
    }

    @JvmField
    val CHARGING_STATION_GENERATE_SPEED = buildServerConfig {
        comment("How much FE energy can charging station generate per tick")
        comment("充电站每游戏刻的发电数量")
        defineInRange("charging_station_generate_speed", 128, 1, Int.MAX_VALUE)
    }

    @JvmField
    val CHARGING_STATION_TRANSFER_SPEED = buildServerConfig {
        comment("How much FE energy can charging station transfer per tick")
        comment("充电站每游戏刻的最大传输能量数值")
        defineInRange("charging_station_transfer_speed", 100000, 1, Int.MAX_VALUE)
    }

    @JvmField
    val CHARGING_STATION_CHARGE_RADIUS = buildServerConfig {
        comment("The charging radius of the charging station")
        comment("充电站的最大传输能量范围")
        defineInRange("charging_station_charge_radius", 8, 0, 128)
    }

    @JvmField
    val CHARGING_STATION_DEFAULT_FUEL_TIME = buildServerConfig {
        comment("The default fuel time of the charging station")
        comment("充电站的默认燃料燃烧时长")
        defineInRange("charging_station_default_fuel_time", 1600, 1, Int.MAX_VALUE)
    }

    @JvmField
    val ARTILLERY_INDICATOR_LIST_SIZE = buildServerConfig {
        comment("The max size of artillery indicator binding list")
        comment("火炮指示器的最大绑定数量")
        defineInRange("artillery_indicator_list_size", 32, 1, Int.MAX_VALUE)
    }

    @JvmField
    val MINE_HITBOX_INVISIBLE = buildServerConfig {
        comment("Set true to make mine hitbox invisible")
        comment("是否隐藏地雷的碰撞箱")
        define("mine_hitbox_invisible", false)
    }

    @JvmField
    val BLUEPRINT_RESEARCH_TABLE_MAX_FUEL = buildServerConfig {
        comment("The max fuel count of blueprint research table")
        comment("蓝图研究台的最大燃料值")
        defineInRange("blueprint_research_table_max_fuel", 8, 1, 128)
    }

    @JvmField
    val SYNC_ENTITY_OVER_RANGE = buildServerConfig {
        push("sync")

        comment("Set true to enable synchronizing client entities with the server")
        comment("是否允许服务端同步超视距的客户端实体")
        define("sync_entity_over_range", true)
    }

    @JvmField
    val SYNC_ENTITY_INTERVAL = buildServerConfig {
        comment("The interval for synchronizing client entities with the server (tick)")
        comment("服务端同步客户端实体的间隔（单位为刻）")
        defineInRange("sync_entity_interval", 10, 1, Int.MAX_VALUE)
    }

    @JvmField
    val CLIENT_SYNC_EXPIRE_TIME = buildServerConfig {
        comment("The expire time for synchronized client entities (ms)")
        comment("同步到客户端的实体的数据失效时间（单位为毫秒）")
        defineInRange("client_sync_expire_time", 1000, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val STEEL_COIL_AWAKE_PERCENTAGE = buildServerConfig {
        comment("The minimum health percentage for activating steel coils")
        comment("激怒钢卷所需的最小生命值比例")
        defineInRange("steel_coil_awake_percentage", 0.99, 0.0, 1.0)
    }

    @JvmField
    val THROW_MEDICAL_KIT = buildServerConfig {
        push("medical kit")

        comment("Set true to enable player throwing medical kits")
        comment("是否允许玩家投掷医疗包")
        define("throw_medical_kit", true)
    }

    @JvmField
    val MEDICAL_KIT_HEAL_AMOUNT = buildServerConfig {
        comment("The healing value of using medical kits")
        comment("使用医疗包的固定治疗数值")
        defineInRange("medical_kit_heal_amount", 5, 0, Int.MAX_VALUE)
    }

    @JvmField
    val MEDICAL_KIT_HEAL_PERCENTAGE = buildServerConfig {
        comment("The percentage of the player's health restored using the medical kit")
        comment("使用医疗包治疗的玩家生命值比例")
        defineInRange("medical_kit_heal_percentage", 0.25, 0.0, 1.0)
    }

    @JvmField
    val MEDICAL_KIT_ENTITY_HEAL_AMOUNT = buildServerConfig {
        comment("The healing value of picking up medical kit entity")
        comment("拾取医疗包实体的固定治疗数值")
        defineInRange("medical_kit_entity_heal_amount", 5, 0, Int.MAX_VALUE)
    }

    @JvmField
    val MEDICAL_KIT_ENTITY_HEAL_PERCENTAGE = buildServerConfig {
        comment("The percentage of the player's health restored picking up medical kit entity")
        comment("拾取医疗包实体治疗的玩家生命值比例")
        defineInRange("medical_kit_entity_heal_percentage", 0.25, 0.0, 1.0).also { pop() }
    }

    @JvmField
    val SMOKE_HIDE_TARGET = buildServerConfig {
        comment("Set true to allow smoke to prevent entities from being set as target")
        comment("是否允许烟雾弹消除生物仇恨")
        define("smoke_hide_target", false).also { pop() }
    }
}