package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance

object TurboCharger : Perk("turbo_charger", Type.FUNCTIONAL) {
    override fun getModifiedCustomRPM(
        rpm: Int,
        data: GunData,
        instance: PerkInstance
    ): Int {
        return (rpm + 5 + 3 * instance.level).coerceAtMost(1200)
    }
}
