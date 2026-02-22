package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity
import software.bernie.geckolib.model.GeoModel

class SwarmDroneModel : GeoModel<SwarmDroneEntity>() {
    override fun getAnimationResource(entity: SwarmDroneEntity) = loc("animations/swarm_drone.animation.json")

    override fun getModelResource(entity: SwarmDroneEntity) = loc("geo/swarm_drone.geo.json")

    override fun getTextureResource(entity: SwarmDroneEntity) = loc("textures/entity/swarm_drone.png")
}
