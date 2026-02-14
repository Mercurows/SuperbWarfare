package com.atsuishio.superbwarfare.client.model.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.armor.UsHelmetPasgtItem
import software.bernie.geckolib.model.GeoModel

class UsHelmetPasgtModel : GeoModel<UsHelmetPasgtItem>() {
    override fun getAnimationResource(item: UsHelmetPasgtItem) = null

    override fun getModelResource(item: UsHelmetPasgtItem?) = loc("geo/us_helmet_pasgt.geo.json")

    override fun getTextureResource(item: UsHelmetPasgtItem?) = loc("textures/armor/us_helmet_pasgt.png")
}
