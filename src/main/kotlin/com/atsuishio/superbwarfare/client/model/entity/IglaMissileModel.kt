package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.IglaMissileEntity
import software.bernie.geckolib.model.GeoModel

class IglaMissileModel : GeoModel<IglaMissileEntity>() {
    override fun getAnimationResource(entity: IglaMissileEntity) = loc("animations/javelin_missile.animation.json")

    override fun getModelResource(entity: IglaMissileEntity) = loc("geo/igla_9k38_missile.geo.json")

    override fun getTextureResource(entity: IglaMissileEntity) = loc("textures/entity/igla_9k38.png")
}
