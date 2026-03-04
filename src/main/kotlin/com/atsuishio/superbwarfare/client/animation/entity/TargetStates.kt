package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition

object TargetStates {
    val INIT: SimpleAnimationState<TargetContext> = SimpleAnimationState.Builder<TargetContext>()
        .evaluatePose { it.getPose() }
        .build()

    val IDLE: SimpleAnimationState<TargetContext> = SimpleAnimationState.Builder<TargetContext>()
        .evaluatePose { it.getPose() }
        .build()

    val DOWN: SimpleAnimationState<TargetContext> = SimpleAnimationState.Builder<TargetContext>()
        .evaluatePose { it.getPose() }
        .build()

    val INIT_TRANS: SimpleTransition<TargetContext> = SimpleTransition.Builder<TargetContext>()
        .predicate { true }
        .target(IDLE)
        .from(INIT)
        .afterTrigger { it.playAnimation("animation.target.idle", AnimationPlayType.LOOP) }
        .build()

    val TO_IDLE: SimpleTransition<TargetContext> = SimpleTransition.Builder<TargetContext>()
        .predicate { !it.isDown() }
        .target(IDLE)
        .from(DOWN)
        .afterTrigger { it.playAnimation("animation.target.idle", AnimationPlayType.LOOP) }
        .build()

    val TO_DOWN: SimpleTransition<TargetContext> = SimpleTransition.Builder<TargetContext>()
        .predicate { it.isDown() }
        .target(DOWN)
        .from(IDLE)
        .afterTrigger { it.playAnimation("animation.target.down", AnimationPlayType.PLAY_ONCE_HOLD) }
        .build()
}