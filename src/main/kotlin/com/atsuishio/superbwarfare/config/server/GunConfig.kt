package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.SERVER_CONFIG
import com.atsuishio.superbwarfare.config.buildServerConfig
import net.minecraftforge.common.ForgeConfigSpec

object GunConfig {

    @JvmField
    val FREE_ATTACHMENT_MODE = buildServerConfig {
        push("gun")

        comment("Set true to ignore attachment mount conflicts (e.g. grip/bayonet + sub-weapon)")
        comment("是否开启自由改装模式：忽略配件挂点组互斥，例如握把与副武器、刺刀与副武器可以同时安装")
        define("free_attachment_mode", false)
    }

    @JvmField
    val FULLY_FREE_ATTACHMENT_MODE = buildServerConfig {
        comment("Set true to allow every gun to use every registered attachment of every slot, ignoring its AvailableAttachments (implies free_attachment_mode)")
        comment("是否开启完全自由改装模式：任意枪械都能安装全部槽位已注册的配件，即使枪械数据里没有声明（蕴含自由改装模式）")
        define("fully_free_attachment_mode", false)
    }

    @JvmField
    val FREE_PERK_MODE = buildServerConfig {
        comment("Set true to allow creative players to install any Perk on any gun, ignoring its AvailablePerks")
        comment("是否开启自由模组模式：创造模式玩家可以给任意枪械安装任意模组，忽略枪械允许的模组列表")
        define("free_perk_mode", true)
    }

    @JvmField
    val FULLY_FREE_PERK_MODE = buildServerConfig {
        comment("Set true to allow players in any game mode to install any Perk on any gun, ignoring its AvailablePerks")
        comment("是否开启完全自由模组模式：任意模式玩家都可以给任意枪械安装任意模组，忽略枪械允许的模组列表")
        define("fully_free_perk_mode", false).also { pop() }
    }

    /** Only bypass weapon compatibility; item costs and upgrade points still follow the player's game mode. */
    @JvmStatic
    fun allowsAnyPerk(creative: Boolean): Boolean =
        configValue(FULLY_FREE_PERK_MODE) || (creative && configValue(FREE_PERK_MODE))

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
