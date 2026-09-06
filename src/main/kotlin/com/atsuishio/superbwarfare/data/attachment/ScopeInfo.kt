package com.atsuishio.superbwarfare.data.attachment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val DEFAULT_SCOPE_VIEW_BONE = "scope_view"
private const val DEFAULT_DIVISION_BONE = "division"

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
 * `modes` lets a single attachment expose multiple selectable optics without changing existing single-mode JSON.
 * Modes may point to different `scope_view`/`division` groups while sharing the ocular geometry.
 */
@Serializable
data class ScopeInfo(
    @SerialName("Type")
    val type: ScopeType = ScopeType.SIGHT,

    @SerialName("ViewRadiusModifier")
    val viewRadiusModifier: Float = 1.0f,

    @SerialName("Modes")
    val modes: List<ScopeMode> = emptyList(),
) {
    fun mode(index: Int = 0): ScopeMode {
        if (modes.isNotEmpty()) {
            return modes[index.coerceIn(modes.indices)]
        }
        return ScopeMode(type, DEFAULT_SCOPE_VIEW_BONE, viewRadiusModifier, DEFAULT_DIVISION_BONE)
    }

    fun modeCount(): Int = if (modes.isEmpty()) 1 else modes.size

    fun supportsModeSwitching(): Boolean = modes.size > 1

    fun isSight(): Boolean = mode().type == ScopeType.SIGHT

    fun isScope(): Boolean = mode().type == ScopeType.SCOPE
}

/**
 * One selectable optic inside a multi-mode scope attachment.
 */
@Serializable
data class ScopeMode(
    @SerialName("Type")
    val type: ScopeType = ScopeType.SIGHT,

    @SerialName("ViewBone")
    val viewBone: String = DEFAULT_SCOPE_VIEW_BONE,

    @SerialName("ViewRadiusModifier")
    val viewRadiusModifier: Float = 1.0f,

    @SerialName("DivisionBone")
    val divisionBone: String = DEFAULT_DIVISION_BONE,

    @SerialName("Zoom")
    val zoom: AttachmentZoom? = null,
) {
    fun isSight(): Boolean = type == ScopeType.SIGHT

    fun isScope(): Boolean = type == ScopeType.SCOPE
}
