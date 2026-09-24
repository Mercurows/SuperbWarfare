package com.atsuishio.superbwarfare.data.gun.melee

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 近战判定形状类型。
 *
 * | Type | 定义 |
 * |---|---|
 * | [CONE] | 目标 AABB 最近点：距离 ≤ `Range`，`\|Δyaw\| ≤ Angle/2`、`\|Δpitch\| ≤ Pitch/2` |
 * | [BOX] | OBB（中心 = 眼睛 + (0, `YOffset`, `ZFrom+Length/2`)，半长 = (`Width/2`, `Height/2`, `Length/2`)，绕 Y 旋转 `yaw`）∩ 目标 AABB |
 * | [CAPSULE] | 线段（沿视线 `ZFrom → ZFrom+Range`）到目标 AABB 的最近距离 ≤ `Radius` |
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
