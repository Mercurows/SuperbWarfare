package com.atsuishio.superbwarfare.client.model.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.armor.RuHelmet6b47Item
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

class RuHelmet6b47Model : GeoModel<RuHelmet6b47Item?>() {
    override fun getAnimationResource(`object`: RuHelmet6b47Item?): ResourceLocation? {
        return null
    }

    override fun getModelResource(`object`: RuHelmet6b47Item?): ResourceLocation {
        return loc("geo/ru_helmet_6b47.geo.json")
    }

    override fun getTextureResource(`object`: RuHelmet6b47Item?): ResourceLocation {
        return loc("textures/armor/ru_helmet_6b47.png")
    }
}
