package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig
import net.minecraft.world.entity.EntityType
import net.minecraftforge.registries.ForgeRegistries

object VehicleConfig {

    @JvmField
    val COLLISION_DESTROY_SOFT_BLOCKS = buildServerConfig {
        push("vehicle")
        push("collision")

        comment("Allows vehicles to destroy soft blocks via collision")
        comment("是否允许载具撞坏柔软方块")
        define("collision_destroy_soft_blocks", false)
    }

    @JvmField
    val COLLISION_DESTROY_NORMAL_BLOCKS = buildServerConfig {
        comment("Allows vehicles to destroy normal blocks via collision")
        comment("是否允许载具撞坏普通方块")
        define("collision_destroy_normal_blocks", false)
    }

    @JvmField
    val COLLISION_DESTROY_HARD_BLOCKS = buildServerConfig {
        comment("Allows vehicles to destroy hard blocks via collision")
        comment("是否允许载具撞坏坚硬方块")
        define("collision_destroy_hard_blocks", false)
    }

    @JvmField
    val COLLISION_DESTROY_BLOCKS_BEASTLY = buildServerConfig {
        comment("Allows vehicles to destroy blocks via collision like a beast")
        comment("是否开启载具野兽撞击模式（只看方块硬度）")
        define("collision_destroy_blocks_beastly", false)
    }

    @JvmField
    val COLLISION_ENTITY_WHITELIST = buildServerConfig {
        comment("List of entities that can be damaged by collision")
        comment("能够被载具撞击的实体白名单")
        defineList("collision_entity_whitelist", listOf<String>()) { it is String }
            .also { pop() }
    }

    @JvmField
    val VEHICLE_ITEM_PICKUP = buildServerConfig {
        comment("Allow vehicles to pick up items")
        comment("是否允许载具拾取物品")
        define("vehicle_item_pickup", true)
    }

    @JvmField
    val VEHICLE_CHUNK_LOADING = buildServerConfig {
        comment("Allow certain vehicles to load chunks")
        comment("是否允许部分载具加载区块")
        define("vehicle_chunk_loading", true)
    }

    @JvmField
    val SAME_TEAM_ENTER_VEHICLE = buildServerConfig {
        comment("Set true to allow only entities in same team to enter vehicle")
        comment("是否仅允许同队伍玩家进入同一辆载具")
        define("same_team_enter_vehicle", true)
    }

    @JvmField
    val COLLECT_DROPS_BY_CRASHING = buildServerConfig {
        comment("Allow vehicles to collect drops after killing other entities by crashing")
        comment("是否允许载具撞击击杀生物后，自动拾取掉落物")
        define("collect_drops_by_crashing", true)
    }

    @JvmField
    val VEHICLE_INFO_DISPLAY_DISTANCE = buildServerConfig {
        comment("Within this distance, the vehicle info will be displayed at client side")
        comment("载具信息显示的最大距离")
        defineInRange("vehicle_info_display_distance", 196, 0, 1024)
    }

    @JvmField
    val SELF_EXPLOSION_DAMAGE = buildServerConfig {
        comment("The damage of self explosion when a vehicle is destroyed")
        comment("载具被击毁时，乘客受到的伤害")
        defineInRange("self_explosion_damage", 114514, 0, Int.MAX_VALUE)
    }

    @JvmField
    val SELF_EXPLOSION_COUNT = buildServerConfig {
        comment("The damage count of self explosion when a vehicle is destroyed")
        comment("载具被击毁时，对乘客造成的伤害次数")
        defineInRange("self_explosion_count", 5, 0, 100)
    }

    @JvmField
    val AIR_CRASH_EXPLOSION_DAMAGE = buildServerConfig {
        comment("The air crash damage when an aircraft is destroyed")
        comment("飞行载具坠机时，乘客受到的伤害")
        defineInRange("air_crash_explosion_damage", 114514, 0, Int.MAX_VALUE)
    }

    @JvmField
    val AIR_CRASH_EXPLOSION_COUNT = buildServerConfig {
        comment("The air crash damage count when an aircraft is destroyed")
        comment("飞行载具坠机时，对乘客造成的伤害次数")
        defineInRange("air_crash_explosion_count", 5, 0, 100)
    }

    @JvmField
    val TURRET_WRECKAGE_LOOT_RATE = buildServerConfig {
        comment("The rate of recycling loot items from turret wreckage")
        comment("从载具炮塔残骸中回收的材料比例")
        defineInRange("turret_wreckage_loot_rate", 0.3, 0.0, 1.0)
    }

    @JvmField
    val SCAN_WHITE_LIST = buildServerConfig {
        comment("List of entity types that can be scanned by radar")
        comment("能够被雷达扫描的实体类型白名单")
        defineList("scan_white_list", listOf("ywzj_vehicle:*")) { true }
    }

    @JvmField
    val TOW_MAX_DISTANCE = buildServerConfig {
        push("towing")

        comment("Maximum straight-line distance for towing two vehicles together")
        comment("两个载具能被拖绳连接的最大直线距离（格）")
        defineInRange("tow_max_distance", 16, 1, 512)
    }

    @JvmField
    val TOW_PULL_DISTANCE = buildServerConfig {
        comment("Minimum distance before a towing vehicle starts pulling the towed vehicle")
        comment("牵引载具开始拉动被牵引载具的最小距离（格）")
        defineInRange("tow_pull_distance", 10, 5, 512)
    }

    @JvmField
    val TOW_BREAK_DISTANCE = buildServerConfig {
        comment("Maximum distance that towline breaks when a vehicle is pulling the towed vehicle. Set to 0 to make it unbreakable")
        comment("拖绳会断裂的最大距离（格），设置成0则不会断裂")
        defineInRange("tow_break_distance", 0, 0, Int.MAX_VALUE)
    }

    @JvmField
    val TOW_BLACK_LIST = buildServerConfig {
        comment("List of entity types that can not be towed by vehicle")
        comment("不能被载具牵引的实体类型名单")
        defineList(
            "tow_black_list", listOf(
                "minecraft:experience_orb",
                "minecraft:primed_tnt",
                "minecraft:ender_pearl",
                "minecraft:interaction",
                "minecraft:marker",
                "minecraft:end_crystal",
                "minecraft:evoker_fangs",
                "minecraft:eye_of_ender",
                "minecraft:falling_block",
                "mts:builder_rendering",
                "evilcraft:vengeance_spirit",
                "touhou_little_maid:power_point",
                "create:carriage_contraption",
                "create:stationary_contraption",
                "create:gantry_contraption",
                "create:super_glue",
                "zombiekit:flares"
            )
        ) { true }
    }

    @JvmField
    val TOW_BAR_EXTRA_LENGTH = buildServerConfig {
        comment("The extra length of tow bar")
        comment("牵引杆的额外长度（格）")
        defineInRange("tow_bar_extra_length", 2, 0, 512).also { pop() }
    }

    @JvmField
    val REPAIR_COOLDOWN = buildServerConfig {
        push("repair")

        comment("The default cooldown of vehicle repair. Set a negative value to disable vehicle repair")
        comment("载具自动回血的触发时间，设置为负数时禁用自动回血")
        defineInRange("repair_cooldown", 200, -1, 100000000)
    }

    @JvmField
    val REPAIR_AMOUNT = buildServerConfig {
        comment("The default amount of health restored per tick when a vehicle is self-repairing")
        comment("载具每游戏刻自动回血的数值")
        defineInRange("repair_amount", 0.05, -100000000.0, 100000000.0).also { pop() }
    }

    @JvmField
    val PASSENGER_Y_OFFSET = buildServerConfig {
        comment("Per-entity Y offset for passenger seating position")
        comment("各实体在载具座位上的Y轴偏移量")
        comment("Format: \"entity_registry_name, offset\"")
        comment("格式: \"entity_registry_name, offset\"")
        comment("Example: [\"touhou_little_maid:maid, 0.75\", \"minecraft:player, -0.2\"]")
        defineList("passenger_y_offset", listOf("touhou_little_maid:maid, 0.75")) { it is String }.also { pop() }
    }

    @JvmStatic
    fun inConfigList(type: EntityType<*>, list: List<String>): Boolean {
        val path = ForgeRegistries.ENTITY_TYPES.getKey(type) ?: return false
        list.forEach {
            if (it.contains(":*")) {
                val namespace = it.split(":*")[0]
                if (path.toString().startsWith(namespace)) return true
            } else {
                if (it == path.toString()) return true
            }
        }
        return false
    }

    @JvmStatic
    fun getPassengerYOffset(type: EntityType<*>): Double {
        val path = ForgeRegistries.ENTITY_TYPES.getKey(type) ?: return 0.0
        val pathStr = path.toString()
        PASSENGER_Y_OFFSET.get().forEach { entry ->
            val parts = entry.split(",").map { it.trim() }
            if (parts.size >= 2 && parts[0] == pathStr) {
                return parts[1].toDoubleOrNull() ?: 0.0
            }
        }
        return 0.0
    }

    @JvmStatic
    fun inScanList(type: EntityType<*>): Boolean {
        return inConfigList(type, SCAN_WHITE_LIST.get())
    }
}