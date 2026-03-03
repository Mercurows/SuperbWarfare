package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition

object SenpaiStates {
    val INIT: SimpleAnimationState<SenpaiContext> = SimpleAnimationState.Builder<SenpaiContext>()
        .evaluatePose { it.getPose() }
        .build()

    val IDLE: SimpleAnimationState<SenpaiContext> = SimpleAnimationState.Builder<SenpaiContext>()
        .evaluatePose { it.getPose() }
        .build()

    val WALK: SimpleAnimationState<SenpaiContext> = SimpleAnimationState.Builder<SenpaiContext>()
        .evaluatePose { it.getPose() }
        .build()

    val RUN: SimpleAnimationState<SenpaiContext> = SimpleAnimationState.Builder<SenpaiContext>()
        .evaluatePose { it.getPose() }
        .build()

    val DIE: SimpleAnimationState<SenpaiContext> = SimpleAnimationState.Builder<SenpaiContext>()
        .evaluatePose { it.getPose() }
        .build()

    val INIT_TRANS: SimpleTransition<SenpaiContext> = SimpleTransition.Builder<SenpaiContext>()
        .predicate { true }
        .target(IDLE)
        .from(INIT)
        .afterTrigger { it.playAnimation("animation.senpai.idle", AnimationPlayType.LOOP) }
        .build()

    val TO_IDLE: SimpleTransition<SenpaiContext> = SimpleTransition.Builder<SenpaiContext>()
        .predicate { !it.isMoving() }
        .target(IDLE)
        .from(WALK, RUN)
        .afterTrigger { it.playAnimation("animation.senpai.idle", AnimationPlayType.LOOP) }
        .build()

    val TO_WALK: SimpleTransition<SenpaiContext> = SimpleTransition.Builder<SenpaiContext>()
        .predicate {
            val entity = it.entity
            val limbSwingAmount = entity.walkAnimation.speed(it.partialTick)
            (it.isMoving() || !(limbSwingAmount > -0.15f && limbSwingAmount < 0.15f)) && !entity.isAggressive
        }
        .target(WALK)
        .from(RUN, IDLE)
        .afterTrigger { it.playAnimation("animation.senpai.walk", AnimationPlayType.LOOP) }
        .build()

    val TO_RUN: SimpleTransition<SenpaiContext> = SimpleTransition.Builder<SenpaiContext>()
        .predicate {
            val entity = it.entity
            entity.isAggressive && it.isMoving()
        }
        .target(RUN)
        .from(WALK, IDLE)
        .afterTrigger {
            if (it.isRunner()) {
                it.playAnimation("animation.senpai.run2", AnimationPlayType.LOOP)
            } else {
                it.playAnimation("animation.senpai.run", AnimationPlayType.LOOP)
            }
        }
        .build()

    val TO_DIE: SimpleTransition<SenpaiContext> = SimpleTransition.Builder<SenpaiContext>()
        .predicate { it.entity.isDeadOrDying }
        .target(DIE)
        .from(RUN, WALK, IDLE)
        .afterTrigger { it.playAnimation("animation.senpai.die", AnimationPlayType.PLAY_ONCE_HOLD) }
        .build()
}
