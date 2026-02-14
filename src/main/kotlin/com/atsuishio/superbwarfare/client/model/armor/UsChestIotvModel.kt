package com.atsuishio.superbwarfare.client.model.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.armor.UsChestIotvItem
import software.bernie.geckolib.model.GeoModel

class UsChestIotvModel : GeoModel<UsChestIotvItem>() {
    override fun getAnimationResource(item: UsChestIotvItem) = null

    override fun getModelResource(item: UsChestIotvItem) = loc("geo/us_chest_iotv.geo.json")

    override fun getTextureResource(item: UsChestIotvItem) = loc("textures/armor/us_chest_iotv.png")
}
