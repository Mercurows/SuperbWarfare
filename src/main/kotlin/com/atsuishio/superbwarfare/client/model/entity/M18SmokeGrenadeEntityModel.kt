package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.M18SmokeGrenadeEntity
import software.bernie.geckolib.model.GeoModel

class M18SmokeGrenadeEntityModel : GeoModel<M18SmokeGrenadeEntity>() {
    override fun getAnimationResource(entity: M18SmokeGrenadeEntity) = null

    override fun getModelResource(entity: M18SmokeGrenadeEntity) = loc("geo/m18_smoke_grenade.geo.json")

    override fun getTextureResource(entity: M18SmokeGrenadeEntity) = loc("textures/item/m_18.png")
}
