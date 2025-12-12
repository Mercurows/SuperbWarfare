package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType

class Attachment(gun: GunData) {

    private val attachment = gun.attachment()

    fun get(type: AttachmentType) = attachment.getInt(type.attachmentName)

    fun set(type: AttachmentType, value: Int) {
        attachment.putInt(type.attachmentName, value)
    }
}
