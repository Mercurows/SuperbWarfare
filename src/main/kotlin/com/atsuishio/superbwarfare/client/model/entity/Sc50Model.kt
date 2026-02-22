package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.Sc50Entity
import software.bernie.geckolib.model.GeoModel

class Sc50Model : GeoModel<Sc50Entity>() {
    override fun getAnimationResource(entity: Sc50Entity) = null

    override fun getModelResource(entity: Sc50Entity) = loc("geo/sc_50.geo.json")

    override fun getTextureResource(entity: Sc50Entity) = loc("textures/entity/ju_87.png")
}
