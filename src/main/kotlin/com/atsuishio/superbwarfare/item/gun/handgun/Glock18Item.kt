package com.atsuishio.superbwarfare.item.gun.handgun

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2

object Glock18Item : GeoGunItemV2(Properties()) {
    override fun hasCustomMagazine(data: GunData): Boolean = true
    override fun hasCustomScope(data: GunData): Boolean = true
    override fun hasCustomBarrel(data: GunData): Boolean = true
    override fun canEditAttachments(data: GunData): Boolean = true
}
