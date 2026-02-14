package com.atsuishio.superbwarfare.client.model.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.armor.UsChestIotvItem
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

class UsChestIotvModel : GeoModel<UsChestIotvItem?>() {
    override fun getAnimationResource(`object`: UsChestIotvItem?): ResourceLocation? {
        return null
    }

    override fun getModelResource(`object`: UsChestIotvItem?): ResourceLocation {
        return loc("geo/us_chest_iotv.geo.json")
    }

    override fun getTextureResource(`object`: UsChestIotvItem?): ResourceLocation {
        return loc("textures/armor/us_chest_iotv.png")
    }
}
