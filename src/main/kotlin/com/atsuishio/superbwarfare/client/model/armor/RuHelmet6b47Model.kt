package com.atsuishio.superbwarfare.client.model.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.armor.RuHelmet6b47Item
import software.bernie.geckolib.model.GeoModel

class RuHelmet6b47Model : GeoModel<RuHelmet6b47Item>() {
    override fun getAnimationResource(item: RuHelmet6b47Item) = null

    override fun getModelResource(item: RuHelmet6b47Item?) = loc("geo/ru_helmet_6b47.geo.json")

    override fun getTextureResource(item: RuHelmet6b47Item?) = loc("textures/armor/ru_helmet_6b47.png")
}
