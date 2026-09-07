package com.atsuishio.superbwarfare.item.gun.handgun

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2

@RegistryName("m_1911")
object M1911Item : GeoGunItemV2(Properties()) {
    override fun hasCustomMagazine(data: GunData): Boolean = true
    override fun hasCustomBarrel(data: GunData): Boolean = true
    override fun canEditAttachments(data: GunData): Boolean = true
}
