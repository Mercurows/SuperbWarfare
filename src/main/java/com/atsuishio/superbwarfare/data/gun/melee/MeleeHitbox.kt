package com.atsuishio.superbwarfare.data.gun.melee

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 近战判定形状（`MeleeHitbox`）。
 *
 * 三种形状共用一份 POJO——没写的字段落回各自类型的默认值。字段级缺省继承
 * （`action.hitbox ?: global.hitbox`）在读取处完成，见 `MeleeAction`。
 *
 * ```jsonc
 * "MeleeHitbox": {
 *   "Type": "Cone",        // Cone | Box | Capsule
 *   "Range": 3.0,
 *   "Angle": 100,          // Cone：水平总张角（度）
 *   "Pitch": 60,           // Cone：垂直总张角（度）；180 = 不限
 *   "Width": 1.4,          // Box：左右全宽
 *   "Height": 1.8,         // Box：上下全高
 *   "YOffset": -0.4,       // Box/Capsule：相对眼睛的垂直偏移
 *   "Length": 2.5,         // Box：前后长度
 *   "ZFrom": 0.0,          // Box/Capsule：沿视线的起点（负值 = 身后）
 *   "Radius": 0.4,         // Capsule：截面半径
 *   "Occlusion": true      // 是否要求视线通畅
 * }
 * ```
 */
@Serializable
data class MeleeHitbox(
    @SerialName("Type")
    val type: MeleeHitboxType = MeleeHitboxType.CONE,

    /** 判定距离（沿视线向上，或圆锥/胶囊的作用半径）。实际值 = 这里写的 Range + 枪的 `MeleeRange` */
    @SerialName("Range")
    val range: Double = 0.0,

    /** Cone：水平总张角（度）。不写时由 [DefaultGunData.meleeAngle] 决定 */
    @SerialName("Angle")
    val angle: Double = 0.0,

    /** Cone：垂直总张角（度）。180 表示不限 */
    @SerialName("Pitch")
    val pitch: Double = 180.0,

    /** Box：左右全宽 */
    @SerialName("Width")
    val width: Double = 1.4,

    /** Box：上下全高 */
    @SerialName("Height")
    val height: Double = 1.8,

    /** Box / Capsule：相对眼睛的垂直偏移 */
    @SerialName("YOffset")
    val yOffset: Double = 0.0,

    /** Box：沿视线的前后长度 */
    @SerialName("Length")
    val length: Double = 2.5,

    /** Box / Capsule：沿视线的起点（负值 = 身后） */
    @SerialName("ZFrom")
    val zFrom: Double = 0.0,

    /** Capsule：截面半径 */
    @SerialName("Radius")
    val radius: Double = 0.4,

    /** 是否要求视线通畅（射线被方块挡住则不打该目标） */
    @SerialName("Occlusion")
    val occlusion: Boolean = true,
) {
    fun rangeOr(fallback: Double) = if (range > 0) range else fallback

    fun angleOr(fallback: Double) = if (angle > 0) angle else fallback
}
