package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition

object DPSGeneratorStates {
    val INIT: SimpleAnimationState<DPSGeneratorContext> = SimpleAnimationState.Builder<DPSGeneratorContext>()
        .evaluatePose { it.getPose() }
        .build()

    val IDLE: SimpleAnimationState<DPSGeneratorContext> = SimpleAnimationState.Builder<DPSGeneratorContext>()
        .evaluatePose { it.getPose() }
        .build()

    val DOWN: SimpleAnimationState<DPSGeneratorContext> = SimpleAnimationState.Builder<DPSGeneratorContext>()
        .evaluatePose { it.getPose() }
        .build()

    val INIT_TRANS: SimpleTransition<DPSGeneratorContext> = SimpleTransition.Builder<DPSGeneratorContext>()
        .predicate { true }
        .target(IDLE)
        .from(INIT)
        .afterTrigger { it.playAnimation("animation.target.idle", AnimationPlayType.LOOP) }
        .build()

    val TO_IDLE: SimpleTransition<DPSGeneratorContext> = SimpleTransition.Builder<DPSGeneratorContext>()
        .predicate { !it.isDown() }
        .target(IDLE)
        .from(DOWN)
        .afterTrigger { it.playAnimation("animation.target.idle", AnimationPlayType.LOOP) }
        .build()

    val TO_DOWN: SimpleTransition<DPSGeneratorContext> = SimpleTransition.Builder<DPSGeneratorContext>()
        .predicate { it.isDown() }
        .target(DOWN)
        .from(IDLE)
        .afterTrigger { it.playAnimation("animation.target.down", AnimationPlayType.PLAY_ONCE_HOLD) }
        .build()
}