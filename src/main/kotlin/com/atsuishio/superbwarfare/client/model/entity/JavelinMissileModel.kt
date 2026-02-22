package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.JavelinMissileEntity
import software.bernie.geckolib.model.GeoModel

class JavelinMissileModel : GeoModel<JavelinMissileEntity>() {
    override fun getAnimationResource(entity: JavelinMissileEntity) = loc("animations/javelin_missile.animation.json")

    override fun getModelResource(entity: JavelinMissileEntity) = loc("geo/javelin_missile.geo.json")

    override fun getTextureResource(entity: JavelinMissileEntity) = loc("textures/entity/javelin_missile.png")
}
