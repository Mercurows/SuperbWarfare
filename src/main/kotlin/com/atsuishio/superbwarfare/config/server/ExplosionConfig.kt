package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object ExplosionConfig {

    @JvmField
    val EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        push("explosion")

        comment("The percentage of explosion damage you take behind cover")
        comment("躲在掩体后时受到的爆炸伤害比例")
        defineInRange("explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val EXPLOSION_DESTROY = buildServerConfig {
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
    val TM_62_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of Tm 62")
        comment("TM62反坦克地雷的爆炸半径")
        defineInRange("tm_62_explosion_radius", 13, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val LUNGE_MINE_EXPLOSION_DAMAGE = buildServerConfig {
        push("Lunge Mine")

        comment("The explosion damage of Lunge Mine")
        comment("突刺爆雷的爆炸伤害")
        defineInRange("lunge_mine_explosion_damage", 60, 1, Int.MAX_VALUE)
    }

    @JvmField
    val LUNGE_MINE_ATTACK_DAMAGE = buildServerConfig {
        comment("The attack damage of Lunge Mine")
        comment("突刺爆雷的直击伤害")
        defineInRange("lunge_mine_attack_damage", 600, 1, Int.MAX_VALUE)
    }

    @JvmField
    val LUNGE_MINE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of Lunge Mine")
        comment("突刺爆雷的爆炸半径")
        defineInRange("lunge_mine_explosion_radius", 4, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val PTKM_1R_EXPLOSION_DAMAGE = buildServerConfig {
        push("Ptkm 1r")

        comment("The explosion damage of Ptkm 1r")
        comment("PTKM1R地雷的爆炸伤害")
        defineInRange("ptkm_1r_explosion_damage", 100, 1, Int.MAX_VALUE)
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
    val PTKM_1R_PROJECTILE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of projectile launched by Ptkm 1r")
        comment("PTKM1R地雷发射的投射物的爆炸半径")
        defineInRange("ptkm_1r_projectile_explosion_radius", 7, 1, Int.MAX_VALUE).also { pop(2) }
    }
}
