package com.atsuishio.superbwarfare.client.model.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.armor.UsHelmetPasgtItem
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

class UsHelmetPasgtModel : GeoModel<UsHelmetPasgtItem?>() {
    override fun getAnimationResource(`object`: UsHelmetPasgtItem?): ResourceLocation? {
        return null
    }

    override fun getModelResource(`object`: UsHelmetPasgtItem?): ResourceLocation {
        return loc("geo/us_helmet_pasgt.geo.json")
    }

    override fun getTextureResource(`object`: UsHelmetPasgtItem?): ResourceLocation {
        return loc("textures/armor/us_helmet_pasgt.png")
    }
}
