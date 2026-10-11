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

    /**
     * 第一人称光束长度（米）——射线的最远距离，也是光束没打中东西时画满的长度。
     *
     * 射线从**玩家眼睛**出发（方向仍是配件自身的出光轴），打中**方块或实体**都会被截断到命中点
     * （光斑画在那里），但不会短于 [minLength]。实体过滤沿用枪的射线那套：旁观、死者、自己和自己的载具
     * 都不挡光，**弹射物（子弹、火箭等）也一律不算**。
     */
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

    /**
     * **被方块截断之后**的绘制长度下限（米）。
     *
     * 贴脸对着墙时命中距离会趋近 0，方管被压成一小截、几乎看不见（激光存在的意义就是那条线），
     * 有了这个下限，方管最短也画到这么长。**光斑跟着方管末端一起走** —— 两者必须一致，
     * 否则近处会出现"方管穿出墙外、光斑单独留在墙上"的断层。
     *
     * 只在**有命中**时才起作用（方块和实体命中都算；第三人称那条短光束不做射线，永远画满
     * [thirdPersonLength]），而且下限本身还会被本次的配置长度压住，不会让光束超过自己声明的长度。
     */
    @SerialName("MinLength")
    val minLength: Float = DEFAULT_MIN_LENGTH,

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

        /** 截断后绘制长度的默认下限（米） */
        const val DEFAULT_MIN_LENGTH: Float = 2f
    }
}