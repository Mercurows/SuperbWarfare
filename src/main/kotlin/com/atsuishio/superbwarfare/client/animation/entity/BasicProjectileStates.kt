package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition
import net.minecraft.world.entity.Entity

object BasicProjectileStates {
    val INIT: SimpleAnimationState<BasicEntityContext<Entity>> =
        SimpleAnimationState.Builder<BasicEntityContext<Entity>>()
            .evaluatePose { it.getPose() }
            .build()

    val IDLE: SimpleAnimationState<BasicEntityContext<Entity>> =
        SimpleAnimationState.Builder<BasicEntityContext<Entity>>()
            .evaluatePose { it.getPose() }
            .build()

    val TO_IDLE: SimpleTransition<BasicEntityContext<Entity>> = SimpleTransition.Builder<BasicEntityContext<Entity>>()
        .predicate { true }
        .target(IDLE)
        .from(INIT)
        .afterTrigger { it.playAnimation("animation.projectile.idle", AnimationPlayType.PLAY_ONCE_HOLD) }
        .build()
}