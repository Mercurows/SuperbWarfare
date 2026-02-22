package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.RgoGrenadeEntity
import software.bernie.geckolib.model.GeoModel

class RgoGrenadeEntityModel : GeoModel<RgoGrenadeEntity>() {
    override fun getAnimationResource(entity: RgoGrenadeEntity) = null

    override fun getModelResource(entity: RgoGrenadeEntity) = loc("geo/rgo_grenade.geo.json")

    override fun getTextureResource(entity: RgoGrenadeEntity) = loc("textures/item/rgo_grenade.png")
}
