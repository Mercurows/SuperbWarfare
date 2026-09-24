package com.atsuishio.superbwarfare.data.gun.melee

import com.atsuishio.superbwarfare.data.gun.melee.MeleeSweep.Companion.AUTO_STEP_DEGREES
import com.atsuishio.superbwarfare.data.gun.melee.MeleeSweep.Companion.MAX_AUTO_STEPS
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * 近战横扫：在 [from] .. [to] 之间按固定步长采样若干次判定，取并集。
 *
 * `From`/`To` 相对**视线 yaw**，方向由符号决定；不写（或 null）= 静态判定（只判一次）。
 *
 * ```jsonc
 * "MeleeSweep": { "From": -75, "To": 75, "Steps": 0 }
 * ```
 */
@Serializable
data class MeleeSweep(
    @SerialName("From")
    val from: Double = 0.0,

    @SerialName("To")
    val to: Double = 0.0,

    /**
     * 采样步数；`<= 0` 表示自动。
     *
     * 自动规则：每 [AUTO_STEP_DEGREES] 度一步，上限 [MAX_AUTO_STEPS]。
     */
    @SerialName("Steps")
    val steps: Int = 0,
) {
    /** 扫掠覆盖的角度跨度 */
    val span: Double get() = abs(to - from)

    /** 解析后的实际采样步数（至少 1 步，即只判一次） */
    fun resolvedSteps(): Int {
        if (steps > 0) return steps
        return ceil(span / AUTO_STEP_DEGREES).toInt().coerceIn(1, MAX_AUTO_STEPS)
    }

    /**
     * 展开成每个采样点的相对 yaw 偏移（度）。
     *
     * 只有 1 步时返回单元素 `[from]`，等价于静态判定。
     */
    fun sampleOffsets(): List<Double> {
        val count = resolvedSteps()
        if (count <= 1) return listOf(from)
        return List(count) { i -> from + (to - from) * i / (count - 1) }
    }

    /** 采样点在视线两侧的最大外扩距离（用于粗筛 AABB 膨胀） */
    fun maxLateral(range: Double): Double = max(abs(from), abs(to)).let { deg ->
        range * kotlin.math.sin(Math.toRadians(deg.coerceAtMost(179.0)))
    }

    companion object {
        /** 自动步长：每 15° 一步 */
        const val AUTO_STEP_DEGREES: Double = 15.0

        /** 自动步数上限 */
        const val MAX_AUTO_STEPS: Int = 8
    }
}
