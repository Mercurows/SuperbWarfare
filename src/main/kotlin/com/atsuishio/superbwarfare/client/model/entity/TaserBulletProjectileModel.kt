package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity
import software.bernie.geckolib.model.GeoModel

class TaserBulletProjectileModel : GeoModel<TaserBulletEntity>() {
    override fun getAnimationResource(entity: TaserBulletEntity) = null

    override fun getModelResource(entity: TaserBulletEntity) = loc("geo/taser_rod.geo.json")

    override fun getTextureResource(entity: TaserBulletEntity) = loc("textures/entity/taser_rod.png")
}
