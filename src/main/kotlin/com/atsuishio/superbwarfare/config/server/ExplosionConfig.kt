package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object ExplosionConfig {

    @JvmField
    val EXPLOSION_DESTROY = buildServerConfig {
        push("explosion")

        comment("Set true to allow Explosion to destroy blocks")
        comment("是否开启爆炸破坏方块")
        define("explosion_destroy", true)
    }

    @JvmField
    val EXTRA_EXPLOSION_EFFECT = buildServerConfig {
        comment("Set true to enable extra explosion effect. For example, C4 and RPG will destroy blocks before explosion")
        comment("是否开启额外破坏效果，例如C4和RPG弹头的破坏方块效果")
        define("extra_explosion_effect", true)
    }

    @JvmField
    val FRIENDLY_MINES = buildServerConfig {
        comment("Set true to allow mines to ignore friendly entities")
        comment("地雷等爆炸物是否会无视友方")
        define("friendly_mines", true)
    }

    @JvmField
    val RGO_GRENADE_EXPLOSION_DAMAGE = buildServerConfig {
        push("RGO Grenade")

        comment("The explosion damage of RGO grenade")
        comment("RGO手榴弹的爆炸伤害")
        defineInRange("rgo_grenade_explosion_damage", 90, 1, 10000000)
    }

    @JvmField
    val RGO_GRENADE_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from RGO grenade")
        comment("RGO手榴弹：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("rgo_grenade_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val RGO_GRENADE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of RGO grenade")
        comment("RGO手榴弹的爆炸半径")
        defineInRange("rgo_grenade_explosion_radius", 5, 1, 50).also { pop() }
    }

    @JvmField
    val M67_GRENADE_EXPLOSION_DAMAGE = buildServerConfig {
        push("M67 Grenade")

        comment("The explosion damage of M67 grenade")
        comment("M67手榴弹的爆炸伤害")
        defineInRange("m67_grenade_explosion_damage", 120, 1, 10000000)
    }

    @JvmField
    val M67_GRENADE_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from M67 grenade")
        comment("M67手榴弹：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("m67_grenade_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val M67_GRENADE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of M67 grenade")
        comment("M67手榴弹的爆炸半径")
        defineInRange("m67_grenade_explosion_radius", 6, 1, 50).also { pop() }
    }

    @JvmField
    val C4_EXPLOSION_COUNTDOWN = buildServerConfig {
        push("C4")

        comment("The explosion countdown of C4")
        comment("定时C4炸弹的爆炸倒计时")
        defineInRange("c4_explosion_countdown", 514, 1, Int.MAX_VALUE)
    }

    @JvmField
    val C4_EXPLOSION_DAMAGE = buildServerConfig {
        comment("The explosion damage of C4")
        comment("C4炸弹的爆炸伤害")
        defineInRange("c4_explosion_damage", 300, 1, Int.MAX_VALUE)
    }

    @JvmField
    val C4_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from C4")
        comment("C4炸弹：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("c4_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val C4_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of C4")
        comment("C4炸弹的爆炸半径")
        defineInRange("c4_explosion_radius", 10, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val CLAYMORE_EXPLOSION_DAMAGE = buildServerConfig {
        push("Claymore")

        comment("The explosion damage of Claymore")
        comment("阔剑地雷的爆炸伤害")
        defineInRange("claymore_explosion_damage", 140, 1, Int.MAX_VALUE)
    }

    @JvmField
    val CLAYMORE_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from Claymore")
        comment("阔剑地雷：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("claymore_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val CLAYMORE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of Claymore")
        comment("阔剑地雷的爆炸半径")
        defineInRange("claymore_explosion_radius", 4, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val BLU_43_EXPLOSION_DAMAGE = buildServerConfig {
        push("Blu 43")

        comment("The explosion damage of Blu 43")
        comment("蝴蝶雷的爆炸伤害")
        defineInRange("blu_43_explosion_damage", 10, 1, Int.MAX_VALUE)
    }

    @JvmField
    val BLU_43_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from Blu 43")
        comment("蝴蝶雷：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("blu_43_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val BLU_43_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of Blu 43")
        comment("蝴蝶雷的爆炸半径")
        defineInRange("blu_43_explosion_radius", 2, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val EDD_EXPLOSION_DAMAGE = buildServerConfig {
        push("EDD")

        comment("The explosion damage of EDD")
        comment("防止攻入装置的爆炸伤害")
        defineInRange("edd_explosion_damage", 60, 1, Int.MAX_VALUE)
    }

    @JvmField
    val EDD_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from EDD")
        comment("EDD地雷：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("edd_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val EDD_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of EDD")
        comment("防止攻入装置的爆炸半径")
        defineInRange("edd_explosion_radius", 3, 1, Int.MAX_VALUE)
    }

    @JvmField
    val EDD_TRACE_RANGE = buildServerConfig {
        comment("The trace range of EDD")
        comment("防止攻入装置的触发距离")
        defineInRange("edd_trace_range", 2, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val TM_62_EXPLOSION_DAMAGE = buildServerConfig {
        push("Tm 62")

        comment("The explosion damage of Tm 62")
        comment("TM62反坦克地雷的爆炸伤害")
        defineInRange("tm_62_explosion_damage", 450, 1, Int.MAX_VALUE)
    }

    @JvmField
    val TM_62_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from TM62")
        comment("TM62地雷：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("tm_62_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val TM_62_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of Tm 62")
        comment("TM62反坦克地雷的爆炸半径")
        defineInRange("tm_62_explosion_radius", 13, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val TYPE_88_CLUSTER_GRENADE_EXPLOSION_DAMAGE = buildServerConfig {
        push("Type 88 Cluster Grenades")

        comment("The explosion damage of Type 88 cluster grenades")
        comment("88式集束手雷的爆炸伤害")
        defineInRange("type_88_cluster_grenade_explosion_damage", 200, 1, 10000000)
    }

    @JvmField
    val TYPE_88_CLUSTER_GRENADE_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from Type 88 cluster grenade")
        comment("88式集束手榴弹：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("type_88_cluster_grenade_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val TYPE_88_CLUSTER_GRENADE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of Type 88 cluster grenades")
        comment("88式集束手雷的爆炸半径")
        defineInRange("type_88_cluster_grenade_explosion_radius", 6, 1, 50)
    }

    @JvmField
    val TYPE_88_CLUSTER_GRENADE_FUSE = buildServerConfig {
        comment("The fuse ticks of Type 88 cluster grenades")
        comment("88式集束手雷的引信时长（tick）")
        defineInRange("type_88_cluster_grenade_fuse", 100, 1, Int.MAX_VALUE)
    }

    @JvmField
    val TYPE_88_CLUSTER_GRENADE_SPLIT_AMOUNT = buildServerConfig {
        comment("The amount of split projectiles spawned by Type 88 cluster grenades")
        comment("88式集束手雷爆炸后分裂出的子弹药数量")
        defineInRange("type_88_cluster_grenade_split_amount", 6, 1, 10).also { pop() }
    }

    @JvmField
    val SPLIT_TYPE_88_GRENADE_EXPLOSION_DAMAGE = buildServerConfig {
        push("Split Type 88 Grenade")

        comment("The explosion damage of split Type 88 grenades")
        comment("88式集束手雷子弹药的爆炸伤害")
        defineInRange("split_type_88_grenade_explosion_damage", 200, 1, 10000000)
    }

    @JvmField
    val SPLIT_TYPE_88_GRENADE_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from split Type 88 grenade")
        comment("88式集束手榴弹破片：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("split_type_88_grenade_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val SPLIT_TYPE_88_GRENADE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of split Type 88 grenades")
        comment("88式集束手雷子弹药的爆炸半径")
        defineInRange("split_type_88_grenade_explosion_radius", 6, 1, 50)
    }

    @JvmField
    val SPLIT_TYPE_88_GRENADE_FUSE = buildServerConfig {
        comment("The fuse ticks of split Type 88 grenades")
        comment("88式集束手雷子弹药的引信时长（tick）")
        defineInRange("split_type_88_grenade_fuse", 20, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val SEA_MINE_EXPLOSION_DAMAGE = buildServerConfig {
        push("SeaMine")
        comment("Sea mine explosion damage")
        comment("水雷爆炸伤害")
        defineInRange("sea_mine_explosion_damage", 500, 0, 10000000)
    }

    @JvmField
    val SEA_MINE_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from sea mine")
        comment("水雷：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("sea_mine_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val SEA_MINE_EXPLOSION_RADIUS = buildServerConfig {
        comment("Sea mine explosion radius in blocks")
        comment("水雷爆炸半径（格）")
        defineInRange("sea_mine_explosion_radius", 6, 1, 256).also { pop() }
    }

    @JvmField
    val HIGH_ENERGY_EXPLOSIVES_BLOCK_EXPLOSION_DAMAGE = buildServerConfig {
        push("HighEnergyExplosivesBlock")
        comment("高能炸药块爆炸伤害")
        comment("High energy explosives block damage")
        defineInRange("high_energy_explosives_block_explosion_damage", 180.0, 0.0, 10000000.0)
    }

    @JvmField
    val HIGH_ENERGY_EXPLOSIVES_BLOCK_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from high energy explosives block")
        comment("高能炸药块：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("high_energy_explosives_block_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val HIGH_ENERGY_EXPLOSIVES_BLOCK_EXPLOSION_RADIUS = buildServerConfig {
        comment("高能炸药块爆炸半径（格）")
        comment("High energy explosives block explosion radius in blocks")
        defineInRange("high_energy_explosives_block_explosion_radius", 12.0, 0.0, 256.0).also { pop() }
    }

    @JvmField
    val BOOBY_TRAP_EXPLOSION_DAMAGE = buildServerConfig {
        push("BoobyTrap")
        comment("伪装诡雷爆炸伤害")
        comment("Booby trap explosion damage")
        defineInRange("booby_trap_explosion_damage", 100.0, 0.0, 10000000.0)
    }

    @JvmField
    val BOOBY_TRAP_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from booby trap")
        comment("诡雷：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("booby_trap_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val BOOBY_TRAP_EXPLOSION_RADIUS = buildServerConfig {
        comment("伪装诡雷爆炸半径（格）")
        comment("Booby trap explosion radius in blocks")
        defineInRange("booby_trap_explosion_radius", 10.0, 0.0, 256.0).also { pop() }
    }

    @JvmField
    val NUCLEAR_BOMB_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        push("Nuclear Bomb")

        comment("The percentage of explosion damage you take behind cover from nuclear bomb")
        comment("核弹：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("nuclear_bomb_explosion_penetration_ratio", 50, 0, 100).also { pop() }
    }

    @JvmField
    val PTKM_1R_EXPLOSION_DAMAGE = buildServerConfig {
        push("Ptkm 1r")

        comment("The explosion damage of Ptkm 1r")
        comment("PTKM1R地雷的爆炸伤害")
        defineInRange("ptkm_1r_explosion_damage", 100, 1, Int.MAX_VALUE)
    }

    @JvmField
    val PTKM_1R_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from PTKM1R mine")
        comment("PTKM1R地雷：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("ptkm_1r_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val PTKM_1R_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of Ptkm 1r")
        comment("PTKM1R地雷的爆炸半径")
        defineInRange("ptkm_1r_explosion_radius", 6, 1, Int.MAX_VALUE)
    }

    @JvmField
    val PTKM_1R_PROJECTILE_HIT_DAMAGE = buildServerConfig {
        comment("The hit damage of projectile launched by Ptkm 1r")
        comment("PTKM1R地雷发射的投射物的直击伤害")
        defineInRange("ptkm_1r_projectile_hit_damage", 500, 1, Int.MAX_VALUE)
    }

    @JvmField
    val PTKM_1R_PROJECTILE_EXPLOSION_DAMAGE = buildServerConfig {
        comment("The explosion damage of projectile launched by Ptkm 1r")
        comment("PTKM1R地雷发射的投射物的爆炸伤害")
        defineInRange("ptkm_1r_projectile_explosion_damage", 80, 1, Int.MAX_VALUE)
    }

    @JvmField
    val PTKM_1R_PROJECTILE_EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        comment("The percentage of explosion damage you take behind cover from PTKM1R projectile")
        comment("PTKM1R地雷的投射物：躲在掩体后时仍会受到的爆炸伤害比例")
        defineInRange("ptkm_1r_projectile_explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val PTKM_1R_PROJECTILE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of projectile launched by Ptkm 1r")
        comment("PTKM1R地雷发射的投射物的爆炸半径")
        defineInRange("ptkm_1r_projectile_explosion_radius", 7, 1, Int.MAX_VALUE).also { pop(2) }
    }
}
