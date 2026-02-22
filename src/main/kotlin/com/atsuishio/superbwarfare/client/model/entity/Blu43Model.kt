package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.Blu43Entity
import software.bernie.geckolib.model.GeoModel

class Blu43Model : GeoModel<Blu43Entity>() {
    override fun getAnimationResource(entity: Blu43Entity) = null

    override fun getModelResource(entity: Blu43Entity) = loc("geo/blu_43.geo.json")

    override fun getTextureResource(entity: Blu43Entity) = loc("textures/entity/blu_43.png")
}
