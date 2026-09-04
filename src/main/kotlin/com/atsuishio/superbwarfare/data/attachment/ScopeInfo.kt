package com.atsuishio.superbwarfare.data.attachment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ScopeType {
    @SerialName("Sight")
    SIGHT,

    @SerialName("Scope")
    SCOPE,
}

/**
 * Scope and sight rendering configuration.
 *
 * Bone names follow a fixed convention in [com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel]:
 * `scope_body`, `ocular_ring`, `ocular*`, and `division*`.
 * Only the optic type and render-time tuning values are data-driven.
 */
@Serializable
data class ScopeInfo(
    @SerialName("Type")
    val type: ScopeType = ScopeType.SIGHT,

    @SerialName("ViewRadiusModifier")
    val viewRadiusModifier: Float = 1.0f,
) {
    fun isSight(): Boolean = type == ScopeType.SIGHT

    fun isScope(): Boolean = type == ScopeType.SCOPE
}
