package com.atsuishio.superbwarfare.client.model.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.armor.GeHelmetM35Item
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

class GeHelmetM35Model : GeoModel<GeHelmetM35Item?>() {
    override fun getAnimationResource(`object`: GeHelmetM35Item?): ResourceLocation? {
        return null
    }

    override fun getModelResource(`object`: GeHelmetM35Item?): ResourceLocation {
        return loc("geo/ge_helmet_m_35.geo.json")
    }

    override fun getTextureResource(`object`: GeHelmetM35Item?): ResourceLocation {
        return loc("textures/armor/ge_helmet_m_35.png")
    }
}
