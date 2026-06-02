package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.living.TargetEntity

class TargetContext(entity: TargetEntity) : BasicEntityContext<TargetEntity>(entity, ANIM) {
    companion object {
        val ANIM = loc("animations/bedrock/entity/target.animation.json")
    }

    fun isDown(): Boolean {
        return entity.downTime > 0
    }
}