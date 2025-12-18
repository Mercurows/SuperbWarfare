package com.atsuishio.superbwarfare.config.client

import com.atsuishio.superbwarfare.config.buildClientConfig

object KillMessageConfig {

    @JvmField
    val SHOW_KILL_MESSAGE = buildClientConfig {
        push("kill_message")

        comment("Set true if you want to show kill message")
        define("show_kill_message", true)
    }

    @JvmField
    val KILL_MESSAGE_POSITION = buildClientConfig {
        comment("The position of kill message")
        defineEnum<KillMessagePosition>("kill_message_position", KillMessagePosition.RIGHT_TOP)
    }

    @JvmField
    val KILL_MESSAGE_COUNT = buildClientConfig {
        comment("The max count of kill messages to show concurrently")
        defineInRange("kill_message_count", 5, 1, 20)
    }

    @JvmField
    val KILL_MESSAGE_MARGIN_X = buildClientConfig {
        comment("The x margin of kill message")
        defineInRange("kill_message_margin_x", 0, -1000, 1000)
    }

    @JvmField
    val KILL_MESSAGE_MARGIN_Y = buildClientConfig {
        comment("The y margin of kill message")
        defineInRange("kill_message_margin_y", 5, -1000, 1000)
    }

    enum class KillMessagePosition {
        LEFT_TOP, LEFT_BOTTOM, RIGHT_TOP, RIGHT_BOTTOM,
    }
}
