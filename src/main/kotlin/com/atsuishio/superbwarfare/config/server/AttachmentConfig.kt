package com.atsuishio.superbwarfare.config.server

import com.atsuishio.superbwarfare.config.SERVER_CONFIG
import com.atsuishio.superbwarfare.config.buildServerConfig
import com.atsuishio.superbwarfare.config.server.AttachmentConfig.FREE_ATTACHMENT_MODE
import com.atsuishio.superbwarfare.config.server.AttachmentConfig.FULLY_FREE_ATTACHMENT_MODE
import net.minecraftforge.common.ForgeConfigSpec

/**
 * 改装（配件）相关的服务端配置。
 *
 * 两档都是**默认关闭**的放宽开关，只改「装得上装不上」的判定，**不动任何配件数据**：
 * 数值加成、模型、动画、枪口焰仍然按配件自己的定义走。
 *
 * | 配置 | 放宽了什么 | 仍然拦着的 |
 * |---|---|---|
 * | [FREE_ATTACHMENT_MODE] | 挂点组互斥与 `ConflictsWith` 互斥（[`AttachmentSlots.conflicts`]） | 配件必须写在这把枪数据的 `AvailableAttachments` 里 |
 * | [FULLY_FREE_ATTACHMENT_MODE] | 上面那条 + `AvailableAttachments`（蕴含自由改装） | 配件物品与配件数据必须都存在 |
 *
 * 判定入口只有两个，两档都收在那里，调用点不许自己再判一次：
 * - [`Attachment.conflict`]：互斥查询（[FULLY_FREE_ATTACHMENT_MODE] 与 [FREE_ATTACHMENT_MODE] 都让它恒返回 `null`）
 * - [`GunData.availableAttachments`]：本枪可用配件表（只有 [FULLY_FREE_ATTACHMENT_MODE] 会换成"全槽位配件"）
 *
 * `/sbw attachment` 与改装界面都从这两个入口取判定，所以配置一开，指令、补全、界面按钮
 * （界面按钮的**显示**见下）会自动跟上。
 *
 * ⚠ **服务端配置不会同步到客户端**（与 `MiscConfig` 里那些被客户端读的项同一现状）：
 * 单人游戏双端共用同一份配置；专用服务器上客户端读的是自己本地的
 * `config/superbwarfare-server.toml`，因此：
 * - **能不能装**永远是服务端说了算（`/sbw attachment` 与改装界面报文都在服务端校验）；
 * - 但改装界面按钮的**可用表现**（`WeaponEditScreen.EditButton.isActive` → `GunItem.hasCustomXxx`）
 *   是客户端本地算的。专用服务器开了完全自由改装、客户端没开时，
 *   枪数据里没声明过配件的槽位在界面上仍是"不可用"，此时用指令安装。
 *
 * ⚠ **关掉配置不会回收已经装上的配件**：两档都只管**新安装**（判定挂在写入路径 `set` / `cycle` 上）。
 * 开着自由改装装出"刺刀 + 消音器"之后再关掉，那把枪**仍然保持**那个组合，只是之后改不回同样的组合；
 * 要清掉就用 `/sbw attachment clear`。
 */
object AttachmentConfig {

    /**
     * 自由改装模式（默认关）：
     *
     * 忽略配件**挂点组互斥**与配件数据里的 `ConflictsWith`，例如刺刀与枪口配件（消音器/制退器）
     * 可以同时装、握把/刺刀/副武器三者可以同时装。
     *
     * 配件**仍然必须**出现在这把枪数据的 `AvailableAttachments` 里 —— 这一档只解决
     * "同一根导轨上抢位置"的问题，不解决"这把枪根本没有这个槽位"的问题，
     * 后者见 [FULLY_FREE_ATTACHMENT_MODE]。
     */
    @JvmField
    val FREE_ATTACHMENT_MODE = buildServerConfig {
        push("attachment")

        comment("Set true to ignore attachment mount conflicts (e.g. bayonet + muzzle device, grip/bayonet + sub-weapon)")
        comment("是否开启自由改装模式：忽略配件挂点组互斥，例如刺刀与枪口配件、握把/刺刀/副武器可以同时安装")
        define("free_attachment_mode", false)
    }

    /**
     * 完全自由改装模式（默认关，**蕴含** [FREE_ATTACHMENT_MODE]）：
     *
     * 任意枪械都能安装该槽位**已注册的全部配件**，即使它的 `AvailableAttachments` 里一条都没写。
     *
     * ⚠ 这一档是给整合包作者 / 调试 / 沙盒玩法准备的，代价是**表现与数值都可能不合理**：
     * - 枪模型里没有对应骨骼（`scope_pos` / `muzzle_pos` / `bayonet_pos` …）时配件**静默不渲染**，
     *   只有数值生效；
     * - 挂上副武器（`SubWeapon` 配件）会真的装配出第二把枪（`SubWeaponRuntime`），
     *   但它的枪口焰/瞄准位形依赖宿主枪模型里的挂点骨骼；
     * - 瞄具的倍率、弹匣的容量都是配件数据自己写的，与宿主枪无关，可能得到很离谱的数值。
     */
    @JvmField
    val FULLY_FREE_ATTACHMENT_MODE = buildServerConfig {
        comment("Set true to allow every gun to use every registered attachment of every slot, ignoring its AvailableAttachments (implies free_attachment_mode)")
        comment("是否开启完全自由改装模式：任意枪械都能安装全部槽位已注册的配件，即使枪械数据里没有声明（蕴含自由改装模式）")
        define("fully_free_attachment_mode", false).also { pop() }
    }

    /**
     * 是否忽略挂点互斥（[FULLY_FREE_ATTACHMENT_MODE] 蕴含本项）。
     *
     * 被 [`Attachment.conflict`] 查询，也是指令 / `Attachment.cycle` 判定"还能不能装"的依据。
     */
    @JvmStatic
    val freeAttachmentMode: Boolean
        get() = configValue(FULLY_FREE_ATTACHMENT_MODE) || configValue(FREE_ATTACHMENT_MODE)

    /** 是否无视 `AvailableAttachments`，任意枪装任意槽位配件。 */
    @JvmStatic
    val fullyFreeAttachmentMode: Boolean
        get() = configValue(FULLY_FREE_ATTACHMENT_MODE)

    /**
     * 读取配置值，**配置尚未加载时退回默认值 `false`**。
     *
     * 数据层（`GunData.availableAttachments` / `Attachment.conflict`）会在任何时刻被查询，
     * 而 Forge 的 `ConfigValue.get()` 在**开发环境**里遇到"配置还没加载"会直接抛异常
     * （生产环境才退回默认值，见 `ForgeConfigSpec.ConfigValue#get`）。这里显式判一次
     * `isLoaded()`，免得数据包加载期的一次查询把游戏炸掉。
     */
    private fun configValue(value: ForgeConfigSpec.BooleanValue): Boolean =
        if (SERVER_CONFIG.isLoaded) value.get() else false
}
