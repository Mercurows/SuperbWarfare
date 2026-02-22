package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity
import software.bernie.geckolib.model.GeoModel

class MediumRocketModel : GeoModel<MediumRocketEntity>() {
    override fun getAnimationResource(entity: MediumRocketEntity) = loc("animations/rpg_rocket.animation.json")

    override fun getModelResource(entity: MediumRocketEntity) = loc("geo/medium_rocket.geo.json")

    override fun getTextureResource(entity: MediumRocketEntity) = loc("textures/entity/type_63.png")
}
