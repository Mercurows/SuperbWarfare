package com.atsuishio.superbwarfare.data.gun.melee

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 近战命中目标的排序方式（`MeleeAction.SortBy`）。
 *
 * 排序结果同时决定 `MaxTargets` 截断与 `Falloff` 衰减顺序。
 */
@Serializable
enum class MeleeSortBy {
    /** 按「眼睛 → 目标 AABB 最近点」与视线的夹角升序（默认，最贴近直觉的「打正前方」） */
    @SerialName("Angle")
    ANGLE,

    /** 按距离升序 */
    @SerialName("Distance")
    DISTANCE,

    /** 按扫掠采样顺序（先被扫到的排前面，同一次采样内按夹角） */
    @SerialName("SweepOrder")
    SWEEP_ORDER,
}
