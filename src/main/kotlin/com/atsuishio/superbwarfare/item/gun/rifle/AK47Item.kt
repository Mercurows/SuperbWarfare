package com.atsuishio.superbwarfare.item.gun.rifle

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2
import net.minecraft.world.item.Rarity

@RegistryName("ak_47")
object AK47Item : GeoGunItemV2(Properties().rarity(Rarity.RARE)) {

    override fun hasCustomGrip(data: GunData): Boolean = true
    override fun hasCustomMagazine(data: GunData): Boolean = true
    override fun hasCustomStock(data: GunData): Boolean = true
    override fun hasCustomScope(data: GunData): Boolean = true
    override fun hasCustomBarrel(data: GunData): Boolean = true

    override fun canEditAttachments(data: GunData): Boolean = true
}
