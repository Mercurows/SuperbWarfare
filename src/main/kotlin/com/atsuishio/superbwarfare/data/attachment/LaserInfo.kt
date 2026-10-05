package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.ModColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LaserInfo(
    /** 出光点所在骨骼名 */
    @SerialName("Bone")
    val bone: String = "laser",

    /** 骨骼上的 locator 名 */
    @SerialName("Locator")
    val locator: String = "laser_muzzle",

    /** 第一人称光束长度（米）。纯视觉量：光束不做射线、不会被方块截断，只是一直画到这里并渐隐 */
    @SerialName("Length")
    val length: Float = DEFAULT_LENGTH,

    /** 光束全宽（米，单层方管的边长，TACZ 语义） */
    @SerialName("Width")
    val width: Float = DEFAULT_WIDTH,

    /** 第三人称右手短光束的长度（米） */
    @SerialName("ThirdPersonLength")
    val thirdPersonLength: Float = DEFAULT_THIRD_PERSON_LENGTH,

    /** 第三人称右手短光束的全宽（米） */
    @SerialName("ThirdPersonWidth")
    val thirdPersonWidth: Float = DEFAULT_THIRD_PERSON_WIDTH,

    /** 默认颜色，可被槽位 tag 的 `LaserColor` 覆盖 */
    @SerialName("Color")
    val color: ModColor = ModColor(FALLBACK_COLOR_RGB),
) {
    /** 本配置默认色的 RGB 部分 */
    fun defaultColorRgb(): Int = color.get() and RGB_MASK

    /**
     * 合成最终颜色：槽位覆盖 > 配件默认 > 兜底红。
     *
     * [override] 传 `Attachment.getLaserColor(slot)`；不在合法域内即视为没设过。
     */
    fun resolveColorRgb(override: Int): Int {
        if (override in 0..RGB_MASK) return override
        return defaultColorRgb()
    }

    companion object {
        const val RGB_MASK: Int = 0xFFFFFF

        /** 谁都没配时用的红色 */
        const val FALLBACK_COLOR_RGB: Int = 0xFF0000

        /** 第一人称光束长度上限，防数据包写出离谱值 */
        const val MAX_LENGTH: Float = 256f

        /** 第一人称光束默认长度（米），与旧的射程默认值一致 */
        const val DEFAULT_LENGTH: Float = 64f

        /** 光束全宽上限（米） */
        const val MAX_WIDTH: Float = 0.5f

        /** 第三人称短光束长度上限（米） */
        const val MAX_THIRD_PERSON_LENGTH: Float = 16f

        /** 单层方管默认全宽（米），取自 TACZ 的 `width` 默认值 */
        const val DEFAULT_WIDTH: Float = 0.008f

        const val DEFAULT_THIRD_PERSON_LENGTH: Float = 2f
        const val DEFAULT_THIRD_PERSON_WIDTH: Float = 0.008f
    }
}