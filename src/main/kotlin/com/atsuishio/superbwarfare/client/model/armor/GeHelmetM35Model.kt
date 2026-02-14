package com.atsuishio.superbwarfare.client.model.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.armor.GeHelmetM35Item
import software.bernie.geckolib.model.GeoModel

class GeHelmetM35Model : GeoModel<GeHelmetM35Item>() {
    override fun getAnimationResource(item: GeHelmetM35Item) = null

    override fun getModelResource(item: GeHelmetM35Item?) = loc("geo/ge_helmet_m_35.geo.json")

    override fun getTextureResource(item: GeHelmetM35Item?) = loc("textures/armor/ge_helmet_m_35.png")
}
