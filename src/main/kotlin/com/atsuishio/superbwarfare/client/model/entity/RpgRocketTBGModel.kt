package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketTBGEntity
import software.bernie.geckolib.model.GeoModel

class RpgRocketTBGModel : GeoModel<RpgRocketTBGEntity>() {
    override fun getAnimationResource(entity: RpgRocketTBGEntity) = loc("animations/rpg_rocket.animation.json")

    override fun getModelResource(entity: RpgRocketTBGEntity) = loc("geo/rpg_rocket_head_tbg.geo.json")

    override fun getTextureResource(entity: RpgRocketTBGEntity) = loc("textures/entity/rpg_rocket_tbg.png")
}
