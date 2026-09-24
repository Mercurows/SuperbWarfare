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
     * 刺刀（枪口卡榫）。
     *
     * 与枪口槽（[BARREL]，消音器/制退器）**不互斥**：两者登记在不同的挂点组上，
     * 见 [com.atsuishio.superbwarfare.data.attachment.AttachmentSlots]。
     */
    @SerialName("Bayonet")
    BAYONET("Bayonet");

    val attachmentName: String = typeName
}
