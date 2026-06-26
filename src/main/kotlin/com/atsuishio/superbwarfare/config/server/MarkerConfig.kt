package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.buildServerConfig

// PJM: серверные переключатели отображения маркеров (хитмаркеры и маркеры над техникой).
// Серверный конфиг синхронизируется на клиент, поэтому значение false жёстко выключает
// соответствующие маркеры у всех игроков, перекрывая их клиентские настройки DisplayConfig.
object MarkerConfig {

    @JvmField
    val ENABLE_HIT_MARKERS = buildServerConfig {
        push("marker")

        comment("Set false to hide hit/headshot/kill markers on the crosshair for everyone")
        comment("是否显示准星上的命中/爆头/击杀指示器")
        define("enable_hit_markers", true)
    }

    @JvmField
    val ENABLE_HIT_SOUND = buildServerConfig {
        comment("Set false to mute the hit/headshot/vehicle hit confirmation sounds for everyone")
        comment("是否播放命中/爆头/载具命中的提示音")
        define("enable_hit_sound", true)
    }

    @JvmField
    val ENABLE_VEHICLE_INFO_MARKER = buildServerConfig {
        comment("Set false to hide the info marker (name/team/health) above vehicles for everyone")
        comment("是否显示载具上方的信息标记（名称/队伍/血量）")
        define("enable_vehicle_info_marker", true)
    }

    @JvmField
    val ENABLE_RPG_LOCK_MARKER = buildServerConfig {
        comment("Set false to hide the RPG lock-on red triangle marker for everyone")
        comment("是否显示RPG锁定目标的红色三角标记")
        define("enable_rpg_lock_marker", true).also { pop() }
    }
}
