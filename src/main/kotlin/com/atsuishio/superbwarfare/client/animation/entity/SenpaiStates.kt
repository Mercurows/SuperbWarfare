package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition

object SenpaiStates {
    val IDLE: SimpleAnimationState<SenpaiContext> = SimpleAnimationState.Builder<SenpaiContext>()
        .onEnter { ctx, _ -> ctx.playAnimation("animation.senpai.idle", AnimationPlayType.LOOP) }
        .evaluatePose { it.getPose() }
        .build()

    val WALK: SimpleAnimationState<SenpaiContext> = SimpleAnimationState.Builder<SenpaiContext>()
        .onEnter { ctx, _ -> ctx.playAnimation("animation.senpai.walk", AnimationPlayType.LOOP) }
        .evaluatePose { it.getPose() }
        .build()

    val RUN: SimpleAnimationState<SenpaiContext> = SimpleAnimationState.Builder<SenpaiContext>()
        .onEnter { ctx, _ ->
            if (ctx.isRunner()) {
                ctx.playAnimation("animation.senpai.run2", AnimationPlayType.LOOP)
            } else {
                ctx.playAnimation("animation.senpai.run", AnimationPlayType.LOOP)
            }
        }
        .evaluatePose { it.getPose() }
        .build()

    val DIE: SimpleAnimationState<SenpaiContext> = SimpleAnimationState.Builder<SenpaiContext>()
        .onEnter { ctx, _ -> ctx.playAnimation("animation.senpai.die", AnimationPlayType.PLAY_ONCE_HOLD) }
        .evaluatePose { it.getPose() }
        .build()

    val TO_IDLE: SimpleTransition<SenpaiContext> = SimpleTransition.Builder<SenpaiContext>()
        .predicate { !it.isMoving() }
        .target(IDLE)
        .from(WALK, RUN)
        .duration(0.6f)
        .build()

    val TO_WALK: SimpleTransition<SenpaiContext> = SimpleTransition.Builder<SenpaiContext>()
        .predicate {
            val entity = it.entity
            val limbSwingAmount = entity.walkAnimation.speed(it.partialTick)
            (it.isMoving() || !(limbSwingAmount > -0.15f && limbSwingAmount < 0.15f)) && !entity.isAggressive
        }
        .target(WALK)
        .from(RUN, IDLE)
        .build()

    val TO_RUN: SimpleTransition<SenpaiContext> = SimpleTransition.Builder<SenpaiContext>()
        .predicate {
            val entity = it.entity
            entity.isAggressive && it.isMoving()
        }
        .target(RUN)
        .from(WALK, IDLE)
        .build()

    val TO_DIE: SimpleTransition<SenpaiContext> = SimpleTransition.Builder<SenpaiContext>()
        .predicate { it.entity.isDeadOrDying }
        .target(DIE)
        .from(RUN, WALK, IDLE)
        .build()
}
