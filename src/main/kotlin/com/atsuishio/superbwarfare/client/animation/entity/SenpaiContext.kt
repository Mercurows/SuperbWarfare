package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.living.SenpaiEntity
import kotlin.math.abs

class SenpaiContext(entity: SenpaiEntity) : BasicEntityContext<SenpaiEntity>(entity, ANIM) {
    companion object {
        val ANIM = loc("animations/bedrock/entity/senpai.animation.json")
    }

    fun isRunner(): Boolean {
        return entity.runner
    }

    fun isMoving(): Boolean {
        val velocity = entity.deltaMovement
        val avgVelocity = (abs(velocity.x) + abs(velocity.z)).toFloat() / 2f
        return avgVelocity > 0.015f
    }
}
