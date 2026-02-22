package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.C4Entity
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

class C4Model : GeoModel<C4Entity>() {
    override fun getAnimationResource(entity: C4Entity) = null

    override fun getModelResource(entity: C4Entity) = loc("geo/c4.geo.json")

    override fun getTextureResource(entity: C4Entity): ResourceLocation {
        val uuid = entity.getUUID()
        if (uuid.leastSignificantBits % 114 == 0L) {
            return loc("textures/item/c4_alter.png")
        }
        return loc("textures/item/c4.png")
    }
}
