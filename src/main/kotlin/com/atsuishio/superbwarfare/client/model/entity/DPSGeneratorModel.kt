package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity
import software.bernie.geckolib.model.GeoModel

class DPSGeneratorModel : GeoModel<DPSGeneratorEntity>() {
    override fun getAnimationResource(entity: DPSGeneratorEntity) = loc("animations/dps_generator.animation.json")

    override fun getModelResource(entity: DPSGeneratorEntity) = loc("geo/dps_generator.geo.json")

    override fun getTextureResource(entity: DPSGeneratorEntity) =
        loc("textures/entity/dps_generator_tier_${entity.generatorLevel.coerceIn(0, 7)}.png")
}
