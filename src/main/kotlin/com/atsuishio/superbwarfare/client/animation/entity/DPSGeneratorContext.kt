package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.resource.BedrockModelLoader

class DPSGeneratorContext(entity: DPSGeneratorEntity) :
    BasicEntityContext<DPSGeneratorEntity>(entity, BedrockModelLoader.DPS_GENERATOR_MA.second) {
    fun isDown(): Boolean {
        return entity.downTime > 0
    }
}