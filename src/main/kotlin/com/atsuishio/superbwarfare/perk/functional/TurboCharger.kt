package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.Perk

object TurboCharger : Perk("turbo_charger", Type.FUNCTIONAL) {
    /**
     * 提升每次开火后叠加的 RPM 增量（5 + 3 * 等级）。
     *
     * 该增量会和枪械数据里自身的 [GunProp.RPM_ADD_AFTER_SHOOT] 相加，
     * 最终由 [GunProp.CUSTOM_RPM_RANGE] 限定上下限，因此这里不需要再自行夹取上限。
     */
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) = with(GunProp) {
        modifier[RPM_ADD_AFTER_SHOOT] += 5 + 3 * modifier.data.perk.getLevel(this@TurboCharger)
    }
}
