package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.SenpaiEntity
import software.bernie.geckolib.model.GeoModel

class SenpaiModel : GeoModel<SenpaiEntity>() {
    override fun getAnimationResource(entity: SenpaiEntity) = loc("animations/senpai.animation.json")

    override fun getModelResource(entity: SenpaiEntity) = loc("geo/senpai.geo.json")

    override fun getTextureResource(entity: SenpaiEntity) = loc("textures/entity/senpai.png")
}
