package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.Mk82Entity
import software.bernie.geckolib.model.GeoModel

class Mk82Model : GeoModel<Mk82Entity>() {
    override fun getAnimationResource(entity: Mk82Entity) = loc("animations/mk82.animation.json")

    override fun getModelResource(entity: Mk82Entity) = loc("geo/mk82.geo.json")

    override fun getTextureResource(entity: Mk82Entity) = loc("textures/entity/mk82.png")
}
