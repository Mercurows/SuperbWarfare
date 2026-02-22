package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketStandardEntity
import software.bernie.geckolib.model.GeoModel

class RpgRocketStandardModel : GeoModel<RpgRocketStandardEntity>() {
    override fun getAnimationResource(entity: RpgRocketStandardEntity) = loc("animations/rpg_rocket.animation.json")

    override fun getModelResource(entity: RpgRocketStandardEntity) = loc("geo/rpg_rocket_head_standard.geo.json")

    override fun getTextureResource(entity: RpgRocketStandardEntity) = loc("textures/entity/rpg_rocket_standard.png")
}
