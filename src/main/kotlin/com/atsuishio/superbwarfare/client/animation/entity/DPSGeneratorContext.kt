package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.BedrockModelLoader
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity

class DPSGeneratorContext(entity: DPSGeneratorEntity) : AbstractContext<DPSGeneratorEntity>(entity, BedrockModelLoader.DPS_GENERATOR_ANI) {
    fun isDown(): Boolean {
        return entity.downTime > 0
    }
}