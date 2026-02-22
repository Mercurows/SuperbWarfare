package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.Kh39Entity
import software.bernie.geckolib.model.GeoModel

class Kh39Model : GeoModel<Kh39Entity>() {
    override fun getAnimationResource(entity: Kh39Entity) = loc("animations/javelin_missile.animation.json")

    override fun getModelResource(entity: Kh39Entity) = loc("geo/kh_39.geo.json")

    override fun getTextureResource(entity: Kh39Entity) = loc("textures/entity/kh_39.png")
}
