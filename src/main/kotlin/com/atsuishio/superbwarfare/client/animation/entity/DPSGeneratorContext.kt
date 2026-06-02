package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity

class DPSGeneratorContext(entity: DPSGeneratorEntity) : BasicEntityContext<DPSGeneratorEntity>(entity, ANIM) {
    companion object {
        val ANIM = loc("animations/bedrock/entity/dps_generator.animation.json")
    }

    fun isDown(): Boolean {
        return entity.downTime > 0
    }
}