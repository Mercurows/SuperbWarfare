package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.TargetEntity
import com.atsuishio.superbwarfare.resource.BedrockModelLoader

class TargetContext(entity: TargetEntity) : AbstractContext<TargetEntity>(entity, BedrockModelLoader.TARGET_ANI) {
    fun isDown(): Boolean {
        return entity.downTime > 0
    }
}