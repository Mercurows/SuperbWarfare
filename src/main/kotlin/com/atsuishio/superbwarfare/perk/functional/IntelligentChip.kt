package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.inventory.menu.ReforgingTableMenu.Companion.MAX_PERK_LEVEL
import com.atsuishio.superbwarfare.perk.Perk

object IntelligentChip : Perk("intelligent_chip", Type.FUNCTIONAL) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        val level = modifier.data.perk.getLevel(this).toInt()
        if (level <= 0) return

        modifier[GunProp.AIM_ASSIST] += level.coerceAtMost(MAX_PERK_LEVEL) * 5
        modifier[GunProp.AUTO_AIM] = true
        modifier[GunProp.AUTO_AIM_RANGE] += 32.0 + 8.0 * (level - 1)
    }
}
