package com.atsuishio.superbwarfare.data.gun.melee

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 近战判定形状类型。
 *
 * 三种形状的**前向长度都是"近战触及距离"**
 * （`(Range + MeleeRange) × 动作的 RangeMultiplier + player.getEntityReach()`）：
 *
 * | Type | 定义 |
 * |---|---|
 * | [BOX] | OBB（中心 = 眼睛 + 局部上偏移 `YOffset` + 视线 × (`ZFrom` + `reach/2`)，半长 = (`Width/2`, `Height/2`, `reach/2`)）∩ 目标 AABB。**默认形状** |
 * | [CONE] | 目标 AABB 最近点：距离 ≤ `reach`，`\|Δyaw\| ≤ Angle/2`、`\|Δpitch\| ≤ Pitch/2` |
 * | [CAPSULE] | 线段（沿视线 `ZFrom → ZFrom + reach`）到目标 AABB 的最近距离 ≤ `Radius` |
 *
 * JSON 里的写法与 [SerialName] 完全一致（大小写敏感）。
 */
@Serializable
enum class MeleeHitboxType {
    @SerialName("Cone")
    CONE,

    @SerialName("Box")
    BOX,

    @SerialName("Capsule")
    CAPSULE,
}
