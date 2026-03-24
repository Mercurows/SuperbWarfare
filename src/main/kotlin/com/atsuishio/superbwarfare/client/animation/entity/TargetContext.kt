package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.resource.BedrockModelLoader

class TargetContext(entity: TargetEntity) :
    BasicEntityContext<TargetEntity>(entity, BedrockModelLoader.TARGET_MA.second) {
    fun isDown(): Boolean {
        return entity.downTime > 0
    }
}