package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.TargetEntity
import software.bernie.geckolib.model.GeoModel

class TargetModel : GeoModel<TargetEntity>() {
    override fun getAnimationResource(entity: TargetEntity) = loc("animations/target.animation.json")

    override fun getModelResource(entity: TargetEntity) = loc("geo/target.geo.json")

    override fun getTextureResource(entity: TargetEntity) = loc("textures/entity/target.png")
}
