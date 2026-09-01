package com.atsuishio.superbwarfare.item.gun.rifle

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2

object AK47V2Item : GeoGunItemV2(Properties()) {

    override fun hasCustomGrip(data: GunData): Boolean = true
    override fun hasCustomMagazine(data: GunData): Boolean = true
    override fun hasCustomStock(data: GunData): Boolean = true
    override fun hasCustomScope(data: GunData): Boolean = true
    override fun hasCustomBarrel(data: GunData): Boolean = true

    override fun canEditAttachments(data: GunData): Boolean = true
}
