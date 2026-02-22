package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.SmallRocketEntity
import software.bernie.geckolib.model.GeoModel

class SmallRocketModel : GeoModel<SmallRocketEntity>() {
    override fun getAnimationResource(entity: SmallRocketEntity) = loc("animations/rpg_rocket.animation.json")

    override fun getModelResource(entity: SmallRocketEntity) = loc("geo/small_rocket.geo.json")

    override fun getTextureResource(entity: SmallRocketEntity) = loc("textures/entity/small_rocket.png")
}
