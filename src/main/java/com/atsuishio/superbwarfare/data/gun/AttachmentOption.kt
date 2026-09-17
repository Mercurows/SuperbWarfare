package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.StringInstanceBuilder
import com.atsuishio.superbwarfare.data.StringOrObjectFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A weapon-level attachment option declared inside AvailableAttachments.
 *
 * A plain string remains valid and is converted into [id] by [Builder].
 * Object form can additionally declare a weapon-specific [override].
 */
@StringOrObjectFactory(AttachmentOption.Builder::class)
@Serializable
data class AttachmentOption(
    @SerialName("Id")
    val id: String = "",

    @SerialName("Override")
    val override: JsonObject? = null,
) {
    object Builder : StringInstanceBuilder<AttachmentOption> {
        override fun fromString(value: String) = AttachmentOption(id = value)
    }
}
