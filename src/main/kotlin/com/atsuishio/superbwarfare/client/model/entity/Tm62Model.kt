package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.Tm62Entity
import software.bernie.geckolib.model.GeoModel

class Tm62Model : GeoModel<Tm62Entity>() {
    override fun getAnimationResource(entity: Tm62Entity) = null

    override fun getModelResource(entity: Tm62Entity) = loc("geo/tm_62.geo.json")

    override fun getTextureResource(entity: Tm62Entity) = loc("textures/entity/tm_62.png")
}
