package com.atsuishio.superbwarfare.data.gun.value

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AttachmentType(typeName: String) {
    @SerialName("Scope")
    SCOPE("Scope"),

    @SerialName("Magazine")
    MAGAZINE("Magazine"),

    @SerialName("Barrel")
    BARREL("Barrel"),

    @SerialName("Stock")
    STOCK("Stock"),

    @SerialName("Grip")
    GRIP("Grip"),

    /**
     * 刺刀
     *
     * 与枪口槽（[BARREL]，消音器/制退器）**互斥**：两者登记在同一个挂点组上，
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
    SUBWEAPON("SubWeapon");

    val attachmentName: String = typeName
}
