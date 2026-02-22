package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity
import software.bernie.geckolib.model.GeoModel

class TurretWreckModel : GeoModel<TurretWreckEntity>() {
    override fun getAnimationResource(entity: TurretWreckEntity) = null

    override fun getModelResource(entity: TurretWreckEntity) = loc("geo/gun_mu.geo.json")

    override fun getTextureResource(entity: TurretWreckEntity) = loc("textures/entity/empty.png")
}
