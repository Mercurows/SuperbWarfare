package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.SERVER_CONFIG
import com.atsuishio.superbwarfare.config.buildServerConfig
import net.minecraftforge.common.ForgeConfigSpec

object AttachmentConfig {

    @JvmField
    val FREE_ATTACHMENT_MODE = buildServerConfig {
        push("attachment")

        comment("Set true to ignore attachment mount conflicts (e.g. grip/bayonet + sub-weapon)")
        comment("是否开启自由改装模式：忽略配件挂点组互斥，例如握把与副武器、刺刀与副武器可以同时安装")
        define("free_attachment_mode", false)
    }

    @JvmField
    val FULLY_FREE_ATTACHMENT_MODE = buildServerConfig {
        comment("Set true to allow every gun to use every registered attachment of every slot, ignoring its AvailableAttachments (implies free_attachment_mode)")
        comment("是否开启完全自由改装模式：任意枪械都能安装全部槽位已注册的配件，即使枪械数据里没有声明（蕴含自由改装模式）")
        define("fully_free_attachment_mode", false).also { pop() }
    }

    /** 是否忽略挂点互斥 */
    @JvmStatic
    val freeAttachmentMode: Boolean
        get() = configValue(FULLY_FREE_ATTACHMENT_MODE) || configValue(FREE_ATTACHMENT_MODE)

    /** 是否无视 `AvailableAttachments`，任意枪装任意槽位配件。 */
    @JvmStatic
    val fullyFreeAttachmentMode: Boolean
        get() = configValue(FULLY_FREE_ATTACHMENT_MODE)

    private fun configValue(value: ForgeConfigSpec.BooleanValue): Boolean =
        if (SERVER_CONFIG.isLoaded) value.get() else false
}
