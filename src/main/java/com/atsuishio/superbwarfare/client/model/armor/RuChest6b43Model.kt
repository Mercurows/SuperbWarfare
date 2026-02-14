package com.atsuishio.superbwarfare.client.model.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.armor.RuChest6b43Item
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

class RuChest6b43Model : GeoModel<RuChest6b43Item?>() {
    override fun getAnimationResource(`object`: RuChest6b43Item?): ResourceLocation? {
        return null
    }

    override fun getModelResource(`object`: RuChest6b43Item?): ResourceLocation {
        return loc("geo/ru_chest_6b43.geo.json")
    }

    override fun getTextureResource(`object`: RuChest6b43Item?): ResourceLocation {
        return loc("textures/armor/ru_chest_6b43.png")
    }
}
