package com.atsuishio.superbwarfare.client.animation.item

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState

object TaserStates {
    val INIT: SimpleAnimationState<TaserContext> = SimpleAnimationState.Builder<TaserContext>()
        .evaluatePose { it.getPose() }
        .build()
}