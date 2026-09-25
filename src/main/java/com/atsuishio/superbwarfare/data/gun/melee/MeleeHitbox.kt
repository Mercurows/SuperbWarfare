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
 *   "Type": "Box",        // Box | Cone | Capsule（不写就是 Box）
 *   "Range": 1.2,         // 近战基础距离；不写时由枪的 MeleeRange 决定
 *   "Width": 1.8,         // Box：左右全宽
 *   "Height": 1.8,        // Box：上下全高
 *   "YOffset": -0.2,      // Box / Capsule：相对眼睛的垂直偏移（沿视线的"上"方向）
 *   "ZFrom": 0.0,         // Box / Capsule：沿视线的起点（负值 = 身后）
 *   "Radius": 0.4,        // Capsule：截面半径
 *   "Angle": 100,         // Cone：水平总张角（度）
 *   "Pitch": 180,         // Cone：垂直总张角（度）；180 = 不限
 *   "Occlusion": true     // 是否要求视线通畅
 * }
 * ```
 *
 * **三种形状的前向长度是同一个量**："近战触及距离" =
 * `(Range + MeleeRange) × 动作的 RangeMultiplier + player.getEntityReach()`。
 * 所以盒子**没有**单独的 `Length` 字段：枪的 `MeleeRange`、配件的距离加成、动作的距离倍率
 * 都能直接作用在判定体长度上，不需要每个形状各写一套。
 * `Angle`/`Pitch` 只有 [MeleeHitboxType.CONE] 用，`Radius` 只有 [MeleeHitboxType.CAPSULE] 用。
 */
@Serializable
data class MeleeHitbox(
    /** 判定形状；不写就是长方体（旧版的圆锥判定已不再是默认形状） */
    @SerialName("Type")
    val type: MeleeHitboxType = MeleeHitboxType.BOX,

    /** 近战基础距离（前向长度）。与枪的 `MeleeRange` 是**叠加**关系 */
    @SerialName("Range")
    val range: Double = 0.0,

    /** Cone：水平总张角（度）。不写时由 `DefaultGunData.meleeAngle` 决定 */
    @SerialName("Angle")
    val angle: Double = 0.0,

    /** Cone：垂直总张角（度）。180 表示不限 */
    @SerialName("Pitch")
    val pitch: Double = 180.0,

    /** Box：左右全宽 */
    @SerialName("Width")
    val width: Double = 1.8,

    /** Box：上下全高 */
    @SerialName("Height")
    val height: Double = 1.8,

    /** Box / Capsule：相对眼睛的垂直偏移 */
    @SerialName("YOffset")
    val yOffset: Double = -0.2,

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
