package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.HandGrenadeEntity
import software.bernie.geckolib.model.GeoModel

class HandGrenadeEntityModel : GeoModel<HandGrenadeEntity>() {
    override fun getAnimationResource(entity: HandGrenadeEntity) = null

    override fun getModelResource(entity: HandGrenadeEntity) = loc("geo/hand_grenade.geo.json")

    override fun getTextureResource(entity: HandGrenadeEntity) = loc("textures/entity/hand_grenade.png")
}
