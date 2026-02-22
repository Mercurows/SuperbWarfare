package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.PtkmProjectileEntity
import software.bernie.geckolib.model.GeoModel

class Ptkm1rProjectileModel : GeoModel<PtkmProjectileEntity>() {
    override fun getAnimationResource(entity: PtkmProjectileEntity) = loc("animations/ptkm_1r.animation.json")

    override fun getModelResource(entity: PtkmProjectileEntity) = loc("geo/ptkm_1r.geo.json")

    override fun getTextureResource(entity: PtkmProjectileEntity) = loc("textures/entity/ptkm_1r.png")
}
