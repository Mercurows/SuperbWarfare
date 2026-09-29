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
    CHARM("Charm");

    val attachmentName: String = typeName
}
