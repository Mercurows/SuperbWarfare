package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition

object BasicProjectileStates {
    val INIT: SimpleAnimationState<BasicProjectileContext<*>> =
        SimpleAnimationState.Builder<BasicProjectileContext<*>>()
            .evaluatePose { it.getPose() }
            .build()

    val IDLE: SimpleAnimationState<BasicProjectileContext<*>> =
        SimpleAnimationState.Builder<BasicProjectileContext<*>>()
            .evaluatePose { it.getPose() }
            .build()

    val INIT_TRANS: SimpleTransition<BasicProjectileContext<*>> = SimpleTransition.Builder<BasicProjectileContext<*>>()
        .predicate { true }
        .target(IDLE)
        .from(INIT)
        .afterTrigger { it.playAnimation("animation.projectile.idle", AnimationPlayType.PLAY_ONCE_HOLD) }
        .build()
}