package com.atsuishio.superbwarfare.data.attachment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val DEFAULT_SCOPE_VIEW_BONE = "scope_view"
private const val DEFAULT_DIVISION_BONE = "division"
private const val SCOPE_BODY_NODE = "scope_body"
private const val OCULAR_NODE = "ocular"
private const val OCULAR_RING_NODE = "ocular_ring"

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

    // 移动时的瞄准倍率，用于能够自动调整焦距的瞄准镜，设置成null相当于禁用该功能
    @SerialName("MovingZoom")
    val movingZoom: Double? = null,

    // 完全瞄准后枪械沿 Z 轴（长度方向）压缩到的比例，默认 0.75
    @SerialName("ZoomLengthScale")
    val zoomLengthScale: Float = 0.75f,

    @SerialName("Modes")
    val modes: List<ScopeMode> = emptyList(),
) {
    fun mode(index: Int = 0): ScopeMode {
        if (modes.isNotEmpty()) {
            return modes[index.coerceIn(modes.indices)]
        }
        return ScopeMode(index = 0, type = type, viewRadiusModifier = viewRadiusModifier, zoomLengthScale = zoomLengthScale)
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
    @SerialName("Index")
    val index: Int = 0,

    @SerialName("Type")
    val type: ScopeType = ScopeType.SIGHT,

    @SerialName("ViewRadiusModifier")
    val viewRadiusModifier: Float = 1.0f,

    // 完全瞄准后枪械沿 Z 轴（长度方向）压缩到的比例，默认 0.75
    @SerialName("ZoomLengthScale")
    val zoomLengthScale: Float = 0.75f,

    @SerialName("Zoom")
    val zoom: AttachmentZoom? = null,
) {
    fun viewBone(): String = nameWithIndex(DEFAULT_SCOPE_VIEW_BONE)

    fun divisionBone(): String = nameWithIndex(DEFAULT_DIVISION_BONE)

    fun scopeBodyBone(): String = nameWithIndex(SCOPE_BODY_NODE)

    fun ocularBone(): String = nameWithIndex(OCULAR_NODE)

    fun ocularRingBone(): String = nameWithIndex(OCULAR_RING_NODE)

    private fun nameWithIndex(base: String): String {
        return if (index > 0) "${base}_$index" else base
    }

    fun isSight(): Boolean = type == ScopeType.SIGHT

    fun isScope(): Boolean = type == ScopeType.SCOPE
}
