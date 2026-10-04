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

    /** 射程（米），未命中时画到这里 */
    @SerialName("Range")
    val range: Float = 64f,

    /** 内芯半宽（米） */
    @SerialName("CoreWidth")
    val coreWidth: Float = DEFAULT_CORE_WIDTH,

    /** 辉光半宽（米） */
    @SerialName("GlowWidth")
    val glowWidth: Float = DEFAULT_GLOW_WIDTH,

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

        /** 射程上限，防数据包写出离谱值 */
        const val MAX_RANGE: Float = 256f

        /** 光束半宽上限（米） */
        const val MAX_HALF_WIDTH: Float = 0.25f

        const val DEFAULT_CORE_WIDTH: Float = 0.010f
        const val DEFAULT_GLOW_WIDTH: Float = 0.028f
    }
}