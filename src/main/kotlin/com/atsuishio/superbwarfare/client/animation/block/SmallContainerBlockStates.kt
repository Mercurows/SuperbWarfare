package com.atsuishio.superbwarfare.client.animation.block

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition

object SmallContainerBlockStates {
    val INIT: SimpleAnimationState<SmallContainerBlockContext> = SimpleAnimationState.Builder<SmallContainerBlockContext>()
        .evaluatePose { it.getPose() }
        .build()

    val OPEN: SimpleAnimationState<SmallContainerBlockContext> = SimpleAnimationState.Builder<SmallContainerBlockContext>()
        .evaluatePose { it.getPose() }
        .build()

    val INIT_TRANS: SimpleTransition<SmallContainerBlockContext> = SimpleTransition.Builder<SmallContainerBlockContext>()
        .predicate { it.isOpen() }
        .target(OPEN)
        .from(INIT)
        .afterTrigger { it.playAnimation("animation.container.open", AnimationPlayType.PLAY_ONCE_HOLD) }
        .build()
}
