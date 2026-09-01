package com.atsuishio.superbwarfare.item.gun.sniper

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2

object K98Item : GeoGunItemV2(Properties()) {
    override fun hasCustomScope(data: GunData): Boolean = true
    override fun hasCustomBarrel(data: GunData): Boolean = true
    override fun canEditAttachments(data: GunData): Boolean = true
}


