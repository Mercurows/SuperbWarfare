package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.Perk

object TurboCharger : Perk("turbo_charger", Type.FUNCTIONAL) {
    /**
     * 提升每次开火后叠加的 RPM 增量（5 + 3 * 等级），并同步抬高自定义射速上限。
     *
     * 实际射速 = `RPM + 累加值`，累加值被 [GunProp.CUSTOM_RPM_MIN] / [GunProp.CUSTOM_RPM_MAX]
     * 限定。默认上限只有 600，不抬高的话涡轮的加成没地方发挥；每级 +30，满级 +600，
     * 正好把默认的 600 顶到 1200（旧版涡轮内置的上限）。
     */
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) = with(GunProp) {
        val level = modifier.data.perk.getLevel(this@TurboCharger)
        modifier[RPM_ADD_AFTER_SHOOT] += 5 + 3 * level
        modifier[CUSTOM_RPM_MAX] += 30 * level
    }
}
