package com.atsuishio.superbwarfare.client.animation.block

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition

object ContainerBlockStates {
    val INIT: SimpleAnimationState<ContainerBlockContext> = SimpleAnimationState.Builder<ContainerBlockContext>()
        .evaluatePose { it.getPose() }
        .build()

    val OPEN: SimpleAnimationState<ContainerBlockContext> = SimpleAnimationState.Builder<ContainerBlockContext>()
        .evaluatePose { it.getPose() }
        .build()

    val INIT_TRANS: SimpleTransition<ContainerBlockContext> = SimpleTransition.Builder<ContainerBlockContext>()
        .predicate { it.isOpen() }
        .target(OPEN)
        .from(INIT)
        .afterTrigger { it.playAnimation("animation.container.open", AnimationPlayType.PLAY_ONCE_HOLD) }
        .build()
}