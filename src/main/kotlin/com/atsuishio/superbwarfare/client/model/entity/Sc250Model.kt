package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.Sc250Entity
import software.bernie.geckolib.model.GeoModel

class Sc250Model : GeoModel<Sc250Entity>() {
    override fun getAnimationResource(entity: Sc250Entity) = null

    override fun getModelResource(entity: Sc250Entity) = loc("geo/sc_250.geo.json")

    override fun getTextureResource(entity: Sc250Entity) = loc("textures/entity/ju_87.png")
}
