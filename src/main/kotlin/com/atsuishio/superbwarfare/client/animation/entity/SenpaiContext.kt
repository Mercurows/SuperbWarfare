package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.BedrockModelLoader
import com.atsuishio.superbwarfare.entity.SenpaiEntity
import kotlin.math.abs

class SenpaiContext(entity: SenpaiEntity) : AbstractContext<SenpaiEntity>(entity, BedrockModelLoader.SENPAI_ANI) {
    fun isRunner(): Boolean {
        return entity.runner
    }

    fun isMoving(): Boolean {
        val velocity = entity.deltaMovement
        val avgVelocity = (abs(velocity.x) + abs(velocity.z)).toFloat() / 2f
        return avgVelocity > 0.015f
    }
}
