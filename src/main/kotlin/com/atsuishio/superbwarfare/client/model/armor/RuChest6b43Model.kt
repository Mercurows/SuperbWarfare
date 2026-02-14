package com.atsuishio.superbwarfare.client.model.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.armor.RuChest6b43Item
import software.bernie.geckolib.model.GeoModel

class RuChest6b43Model : GeoModel<RuChest6b43Item>() {
    override fun getAnimationResource(item: RuChest6b43Item) = null

    override fun getModelResource(item: RuChest6b43Item?) = loc("geo/ru_chest_6b43.geo.json")

    override fun getTextureResource(item: RuChest6b43Item?) = loc("textures/armor/ru_chest_6b43.png")
}
