package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

object SpawnConfig {

    @JvmField
    val SPAWN_SENPAI = buildServerConfig {
        push("spawn")

        comment("Set true to allow Senpai to spawn naturally")
        comment("是否允许野兽先辈自然生成（喜）")
        define("spawn_senpai", false)
    }

    @JvmField
    val SPAWN_CREEPING_SENPAI = buildServerConfig {
        comment("Set true to allow Creeping Senpai to spawn naturally")
        comment("是否允许爬行先辈自然生成（悲）")
        define("spawn_creeping_senpai", false)
    }

    @JvmField
    val SPAWN_STEEL_COIL = buildServerConfig {
        comment("Set true to allow Steel Coil to spawn naturally")
        comment("是否允许钢卷自然生成")
        define("spawn_steel_coil", false)
    }

    @JvmField
    val SPAWN_MOB_WITH_GUNS = buildServerConfig {
        comment("Set true to allow mobs to spawn with guns, configured by data packs (sbw/mob_guns)")
        comment("是否允许生物生成时携带枪械，由数据包 sbw/mob_guns 配置")
        define("spawn_mob_with_guns", false)
    }

    @JvmField
    val MOB_GUN_DROP_CHANCE = buildServerConfig {
        comment("Default chance for a mob carrying a gun to drop it, 0 to disable mob gun drops entirely")
        comment("Each entry of sbw/mob_guns can override this with its own \"Drop\".\"Chance\"")
        comment("持枪生物掉落枪械的默认概率，0 为全局关闭掉落")
        comment("sbw/mob_guns 的每条策略可以用 \"Drop\".\"Chance\" 单独覆盖")
        defineInRange("mob_gun_drop_chance", 0.085, 0.0, 1.0)
    }

    @JvmField
    val MOB_GUN_DROP_CLEAR_AMMO = buildServerConfig {
        comment("Default: whether a dropped mob gun is emptied (magazine and overflow reserve) before it drops")
        comment("Each entry of sbw/mob_guns can override this with its own \"Drop\".\"ClearAmmo\"")
        comment("Note: a mob's backup ammo lives on the mob itself and never drops.")
        comment("掉落的枪械是否默认清空弹药（弹匣与溢出备弹）")
        comment("sbw/mob_guns 的每条策略可以用 \"Drop\".\"ClearAmmo\" 单独覆盖")
        comment("注意：生物的备弹保存在生物身上，不会随掉落物一起掉出")
        define("mob_gun_drop_clear_ammo", true).also { pop() }
    }
}
