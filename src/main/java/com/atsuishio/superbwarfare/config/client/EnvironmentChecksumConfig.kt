package com.atsuishio.superbwarfare.config.client

import com.atsuishio.superbwarfare.config.buildClientConfig

object EnvironmentChecksumConfig {

    @JvmField
    val ENVIRONMENT_CHECKSUM = buildClientConfig {
        push("checksum")

        comment("System environment checksum, do not edit")
        define("environment_checksum", "").also { pop() }
    }

}
