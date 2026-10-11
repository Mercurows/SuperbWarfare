package com.atsuishio.superbwarfare.client.renderer.ammo

import com.atsuishio.superbwarfare.data.attachment.AmmoBarEntry
import com.atsuishio.superbwarfare.data.attachment.AmmoTextEntry

/** 本帧模型上所有弹药条 / 弹药文字要显示的数值 */
data class AmmoReadout(
    val bars: List<AmmoBarEntry> = emptyList(),
    val texts: List<AmmoTextEntry> = emptyList(),
    /** 弹药条的进度，`1` 为满、`0` 为空 */
    val progress: Float = 1f,
    /** `%ammo_count%` 展开成的数字 */
    val count: Int = 0,
    /** `%range%` 展开成的距离，单位格；[AmmoTextEntry.NO_RANGE] 表示本帧没有读数 */
    val range: Int = AmmoTextEntry.NO_RANGE,
    /** `%heat%` 展开成的热量，`0` ~ `100` */
    val heat: Int = 0,
) {
    /** 模型没配任何弹药显示时为 `true` */
    val isEmpty: Boolean get() = bars.isEmpty() && texts.isEmpty()
}
