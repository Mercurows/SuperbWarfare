package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.living.SenpaiEntity
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import kotlin.math.abs

class SenpaiContext(entity: SenpaiEntity) :
    BasicEntityContext<SenpaiEntity>(entity, BedrockModelLoader.SENPAI_MA.second) {
    fun isRunner(): Boolean {
        return entity.runner
    }

    fun isMoving(): Boolean {
        val velocity = entity.deltaMovement
        val avgVelocity = (abs(velocity.x) + abs(velocity.z)).toFloat() / 2f
        return avgVelocity > 0.015f
    }
}
