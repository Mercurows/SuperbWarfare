package com.atsuishio.superbwarfare.data.gun.value

enum class AttachmentType(typeName: String) {
    SCOPE("Scope"),
    MAGAZINE("Magazine"),
    BARREL("Barrel"),
    STOCK("Stock"),
    GRIP("Grip");

    val attachmentName: String = typeName
}
