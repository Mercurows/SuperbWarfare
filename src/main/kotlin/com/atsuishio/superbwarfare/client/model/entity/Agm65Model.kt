package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.Agm65Entity
import software.bernie.geckolib.model.GeoModel

class Agm65Model : GeoModel<Agm65Entity>() {
    override fun getAnimationResource(entity: Agm65Entity) = loc("animations/javelin_missile.animation.json")

    override fun getModelResource(entity: Agm65Entity) = loc("geo/agm65.geo.json")

    override fun getTextureResource(entity: Agm65Entity) = loc("textures/entity/agm65.png")
}
