package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.entity.projectile.Ptkm1rEntity
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition

object Ptkm1rStates {
    val INIT: SimpleAnimationState<BasicEntityContext<Ptkm1rEntity>> =
        SimpleAnimationState.Builder<BasicEntityContext<Ptkm1rEntity>>()
            .evaluatePose { it.getPose() }
            .build()

    val DEPLOYED: SimpleAnimationState<BasicEntityContext<Ptkm1rEntity>> =
        SimpleAnimationState.Builder<BasicEntityContext<Ptkm1rEntity>>()
            .evaluatePose { it.getPose() }
            .build()

    val INIT_TRANS: SimpleTransition<BasicEntityContext<Ptkm1rEntity>> =
        SimpleTransition.Builder<BasicEntityContext<Ptkm1rEntity>>()
            .predicate { true }
            .target(DEPLOYED)
            .from(INIT)
            .afterTrigger { it.playAnimation("animation.ptkm_1r.deploy", AnimationPlayType.PLAY_ONCE_HOLD) }
            .build()
}