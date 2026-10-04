package com.atsuishio.superbwarfare.data.gun.value

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AttachmentType(typeName: String) {
    @SerialName("Scope")
    SCOPE("Scope"),

    @SerialName("Magazine")
    MAGAZINE("Magazine"),

    @SerialName("Muzzle")
    MUZZLE("Muzzle"),

    @SerialName("Stock")
    STOCK("Stock"),

    @SerialName("Grip")
    GRIP("Grip"),

    /**
     * 刺刀
     *
     * 与枪口槽（[MUZZLE]，消音器/制退器）**互斥**：两者登记在同一个挂点组上，
     * 装了其中一个就装不了另一个，见 [com.atsuishio.superbwarfare.data.attachment.AttachmentSlots]。
     */
    @SerialName("Bayonet")
    BAYONET("Bayonet"),

    /**
     * 副武器（下挂榴弹发射器这类）。
     *
     * 槽位本身不代表"副武器身份"：**身份由配件数据里的 `SubWeapon` 定义决定**
     * （`AttachmentDefinition.subWeapon`）——这个槽位只是"下挂件默认住在这里"，
     * 任何槽位的配件只要写了 `SubWeapon` 就算副武器。
     */
    @SerialName("SubWeapon")
    SUBWEAPON("SubWeapon"),

    /**
     * 吊坠 / 挂件。
     *
     * 与其它槽位最大的区别是**它会在第一人称下摆动**：配件模型按
     * `fixed`（固定件）/ `string`（连接绳）/ `charm`（挂件本体）三组约定制作，
     * 后两组绕 `charm_pos` 摆点做摆锤运动，物理参数见
     * [com.atsuishio.superbwarfare.data.attachment.CharmInfo]。
     *
     * 它挂在**自己的挂点组**（`charm_loop`）上，与瞄具/刺刀/握把都不互斥 ——
     * 吊坠本来就是挂在枪身侧面的一个小环上。
     */
    @SerialName("Charm")
    CHARM("Charm"),

    /**
     * 下导轨配件（脚架这类挂在护木下方导轨上的东西）。
     *
     * 与 [GRIP] 物理上是同一根下导轨，但**各自登记在自己的挂点组上**（`lower_rail` / `grip_rail`），
     * 所以两者不互斥、可以同时装 —— 合并挂点组会让"装了垂直握把就装不了脚架"，
     * 那是玩法改动，见 [com.atsuishio.superbwarfare.data.attachment.AttachmentSlots]。
     */
    @SerialName("LowerRail")
    LOWER_RAIL("LowerRail"),

    /**
     * 上导轨配件（挂在护木/机匣上方的导轨上，常见的是激光指示器、战术手电这类）。
     *
     * 占自己的挂点组 `upper_rail`，与其它任何槽位都不互斥 —— 上下左右四根导轨在枪上是四个
     * 互不相干的位置（见 [LOWER_RAIL] 的说明，挂点组只表示"占的是同一处"）。
     * **目前还没有任何配件住在这些导轨槽位上**，槽位先建好，等配件做出来直接往里放即可。
     */
    @SerialName("UpperRail")
    UPPER_RAIL("UpperRail"),

    /**
     * 左导轨配件（挂在护木左侧的导轨上）。
     *
     * 与 [RIGHT_RAIL] 分属两个挂点组（`left_rail` / `right_rail`），**左右不互斥**：它们是护木两侧
     * 两个独立的位置，同时装一个是正常玩法。目前还没有配件住在这些槽位上。
     */
    @SerialName("LeftRail")
    LEFT_RAIL("LeftRail"),

    /**
     * 右导轨配件（挂在护木右侧的导轨上）。与 [LEFT_RAIL] 对称，两者可以共存。
     * 目前还没有配件住在这些槽位上。
     */
    @SerialName("RightRail")
    RIGHT_RAIL("RightRail");

    val attachmentName: String = typeName
}
