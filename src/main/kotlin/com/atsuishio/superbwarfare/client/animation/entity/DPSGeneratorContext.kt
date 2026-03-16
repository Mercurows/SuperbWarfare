package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity
import com.atsuishio.superbwarfare.resource.BedrockModelLoader

class DPSGeneratorContext(entity: DPSGeneratorEntity) : BasicEntityContext<DPSGeneratorEntity>(entity, BedrockModelLoader.DPS_GENERATOR_ANI) {
    fun isDown(): Boolean {
        return entity.downTime > 0
    }
}