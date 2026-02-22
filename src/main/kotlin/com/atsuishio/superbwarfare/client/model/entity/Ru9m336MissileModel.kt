package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.Ru9m336MissileEntity
import software.bernie.geckolib.model.GeoModel

class Ru9m336MissileModel : GeoModel<Ru9m336MissileEntity>() {
    override fun getAnimationResource(entity: Ru9m336MissileEntity) = loc("animations/javelin_missile.animation.json")

    override fun getModelResource(entity: Ru9m336MissileEntity) = loc("geo/igla_9k38_missile.geo.json")

    override fun getTextureResource(entity: Ru9m336MissileEntity) = loc("textures/entity/igla_9k38.png")
}
