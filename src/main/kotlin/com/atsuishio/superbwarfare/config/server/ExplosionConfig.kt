package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object ExplosionConfig {

    @JvmField
    val EXPLOSION_PENETRATION_RATIO = buildServerConfig {
        push("explosion")

        comment("The percentage of explosion damage you take behind cover")
        defineInRange("explosion_penetration_ratio", 15, 0, 100)
    }

    @JvmField
    val EXPLOSION_DESTROY = buildServerConfig {
        comment("Set true to allow Explosion to destroy blocks")
        define("explosion_destroy", true)
    }

    @JvmField
    val EXTRA_EXPLOSION_EFFECT = buildServerConfig {
        comment("Set true to enable extra explosion effect. For example, C4 and RPG will destroy blocks before explosion")
        define("extra_explosion_effect", true)
    }

    @JvmField
    val RGO_GRENADE_EXPLOSION_DAMAGE = buildServerConfig {
        push("RGO Grenade")

        comment("The explosion damage of RGO grenade")
        defineInRange("rgo_grenade_explosion_damage", 90, 1, 10000000)
    }

    @JvmField
    val RGO_GRENADE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of RGO grenade")
        defineInRange("rgo_grenade_explosion_radius", 5, 1, 50).also { pop() }
    }

    @JvmField
    val M67_GRENADE_EXPLOSION_DAMAGE = buildServerConfig {
        push("M67 Grenade")

        comment("The explosion damage of M67 grenade")
        defineInRange("m67_grenade_explosion_damage", 120, 1, 10000000)
    }

    @JvmField
    val M67_GRENADE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of M67 grenade")
        defineInRange("m67_grenade_explosion_radius", 6, 1, 50).also { pop() }
    }

    @JvmField
    val C4_EXPLOSION_COUNTDOWN = buildServerConfig {
        push("C4")

        comment("The explosion countdown of C4")
        defineInRange("c4_explosion_countdown", 514, 1, Int.MAX_VALUE)
    }

    @JvmField
    val C4_EXPLOSION_DAMAGE = buildServerConfig {
        comment("The explosion damage of C4")
        defineInRange("c4_explosion_damage", 300, 1, Int.MAX_VALUE)
    }

    @JvmField
    val C4_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of C4")
        defineInRange("c4_explosion_radius", 10, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val CLAYMORE_EXPLOSION_DAMAGE = buildServerConfig {
        push("Claymore")

        comment("The explosion damage of Claymore")
        defineInRange("claymore_explosion_damage", 140, 1, Int.MAX_VALUE)
    }

    @JvmField
    val CLAYMORE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of Claymore")
        defineInRange("claymore_explosion_radius", 4, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val TM_62_EXPLOSION_DAMAGE = buildServerConfig {
        push("Tm 62")

        comment("The explosion damage of Tm 62")
        defineInRange("tm_62_explosion_damage", 450, 1, Int.MAX_VALUE)
    }

    @JvmField
    val TM_62_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of Tm 62")
        defineInRange("tm_62_explosion_radius", 13, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val LUNGE_MINE_EXPLOSION_DAMAGE = buildServerConfig {
        push("Lunge Mine")

        comment("The explosion damage of Lunge Mine")
        defineInRange("lunge_mine_explosion_damage", 60, 1, Int.MAX_VALUE)
    }

    @JvmField
    val LUNGE_MINE_ATTACK_DAMAGE = buildServerConfig {
        comment("The attack damage of Lunge Mine")
        defineInRange("lunge_mine_attack_damage", 600, 1, Int.MAX_VALUE)
    }

    @JvmField
    val LUNGE_MINE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of Lunge Mine")
        defineInRange("lunge_mine_explosion_radius", 4, 1, Int.MAX_VALUE).also { pop() }
    }

    @JvmField
    val PTKM_1R_EXPLOSION_DAMAGE = buildServerConfig {
        push("Ptkm 1r")

        comment("The explosion damage of Ptkm 1r")
        defineInRange("ptkm_1r_explosion_damage", 100, 1, Int.MAX_VALUE)
    }

    @JvmField
    val PTKM_1R_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of Ptkm 1r")
        defineInRange("ptkm_1r_explosion_radius", 6, 1, Int.MAX_VALUE)
    }

    @JvmField
    val PTKM_1R_PROJECTILE_HIT_DAMAGE = buildServerConfig {
        comment("The hit damage of projectile launched by Ptkm 1r")
        defineInRange("ptkm_1r_projectile_hit_damage", 500, 1, Int.MAX_VALUE)
    }

    @JvmField
    val PTKM_1R_PROJECTILE_EXPLOSION_DAMAGE = buildServerConfig {
        comment("The explosion damage of projectile launched by Ptkm 1r")
        defineInRange("ptkm_1r_projectile_explosion_damage", 80, 1, Int.MAX_VALUE)
    }

    @JvmField
    val PTKM_1R_PROJECTILE_EXPLOSION_RADIUS = buildServerConfig {
        comment("The explosion radius of projectile launched by Ptkm 1r")
        defineInRange("ptkm_1r_projectile_explosion_radius", 7, 1, Int.MAX_VALUE).also { pop(2) }
    }
}
