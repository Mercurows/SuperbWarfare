package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.DeserializeFromString
import com.atsuishio.superbwarfare.data.STOFactory
import com.atsuishio.superbwarfare.data.StringInstanceBuilder
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedGsonObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A weapon-level attachment option declared inside AvailableAttachments.
 *
 * A plain string remains valid and is converted into [id] by [Builder].
 * Object form can additionally declare a weapon-specific [override].
 */
@STOFactory(AttachmentOption.Builder::class)
@Serializable
class AttachmentOption : DeserializeFromString {
    @SerialName("Id")
    var id: String = ""

    @SerialName("Override")
    var override: SerializedGsonObject? = null

    override fun deserializeFromString(str: String?) {
        id = str.orEmpty()
    }

    object Builder : StringInstanceBuilder<AttachmentOption> {
        override fun fromString(value: String) = AttachmentOption().apply {
            id = value
        }
    }
}
