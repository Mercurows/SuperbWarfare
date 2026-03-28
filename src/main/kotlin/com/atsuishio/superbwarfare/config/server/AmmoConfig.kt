package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.SERVER_CONFIG_BUILDER
import com.atsuishio.superbwarfare.config.buildServerConfig
import com.atsuishio.superbwarfare.data.gun.Ammo

fun Ammo.limit(): Int = AmmoConfig.AMMO_LIMIT[this]!!.get()
fun Ammo.ammoBoxLimit(): Int = AmmoConfig.AMMO_BOX_LIMIT[this]!!.get()

object AmmoConfig {

    init {
        SERVER_CONFIG_BUILDER.push("ammo")
    }

    val AMMO_LIMIT = Ammo.entries.associateWith {
        buildServerConfig {
            comment("The max ammo limit of ${it.serializationName}")
            comment("玩家身上能存储的最大弹药数量")
            defineInRange("${it.name}_ammo_limit", Int.MAX_VALUE, 0, Int.MAX_VALUE)
        }
    }

    val AMMO_BOX_LIMIT = Ammo.entries.associateWith {
        buildServerConfig {
            comment("The max ammo limit for ammo boxes of ${it.serializationName}")
            comment("弹药箱能存储的最大弹药数量")
            defineInRange("${it.name}_ammo_box_limit", Int.MAX_VALUE, 0, Int.MAX_VALUE)
        }
    }

    init {
        SERVER_CONFIG_BUILDER.pop()
    }

}
