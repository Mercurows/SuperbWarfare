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
    BAYONET("Bayonet");

    val attachmentName: String = typeName
}
