package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.WireGuideMissileEntity
import software.bernie.geckolib.model.GeoModel

class WireGuideMissileModel : GeoModel<WireGuideMissileEntity>() {
    override fun getAnimationResource(entity: WireGuideMissileEntity) = loc("animations/javelin_missile.animation.json")

    override fun getModelResource(entity: WireGuideMissileEntity) = loc("geo/wire_guide_missile.geo.json")

    override fun getTextureResource(entity: WireGuideMissileEntity) = loc("textures/entity/javelin_missile.png")
}
