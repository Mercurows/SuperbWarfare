package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.MedicalKitEntity
import software.bernie.geckolib.model.GeoModel

class MedicalKitModel : GeoModel<MedicalKitEntity>() {
    override fun getAnimationResource(entity: MedicalKitEntity) = null

    override fun getModelResource(entity: MedicalKitEntity) = loc("geo/medical_kit.geo.json")

    override fun getTextureResource(entity: MedicalKitEntity) = loc("textures/entity/medical_kit.png")
}
