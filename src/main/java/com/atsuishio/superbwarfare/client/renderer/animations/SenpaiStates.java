package com.atsuishio.superbwarfare.client.renderer.animations;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition;

public class SenpaiStates {
    public static final SimpleAnimationState<SenpaiContext> IDLE = new SimpleAnimationState.Builder<SenpaiContext>()
            .onEnter((ctx, state) -> ctx.playAnimation("animation.senpai.idle", AnimationPlayType.LOOP))
            .evaluatePose(SenpaiContext::getPose)
            .build();

    public static final SimpleAnimationState<SenpaiContext> WALK = new SimpleAnimationState.Builder<SenpaiContext>()
            .onEnter((ctx, state) -> ctx.playAnimation("animation.senpai.walk", AnimationPlayType.LOOP))
            .evaluatePose(SenpaiContext::getPose)
            .build();

    public static final SimpleAnimationState<SenpaiContext> RUN = new SimpleAnimationState.Builder<SenpaiContext>()
            .onEnter((ctx, state) -> {
                if (ctx.isRunner()) {
                    ctx.playAnimation("animation.senpai.run2", AnimationPlayType.LOOP);
                } else {
                    ctx.playAnimation("animation.senpai.run", AnimationPlayType.LOOP);
                }
            })
            .evaluatePose(SenpaiContext::getPose)
            .build();

    public static final SimpleAnimationState<SenpaiContext> DIE = new SimpleAnimationState.Builder<SenpaiContext>()
            .onEnter((ctx, state) -> ctx.playAnimation("animation.senpai.die", AnimationPlayType.PLAY_ONCE_HOLD))
            .evaluatePose(SenpaiContext::getPose)
            .build();

    public static final SimpleTransition<SenpaiContext> TO_IDLE = new SimpleTransition.Builder<SenpaiContext>()
            .predicate(ctx -> !ctx.isMoving())
            .target(IDLE)
            .from(WALK, RUN)
            .duration(0.6f)
            .build();

    public static final SimpleTransition<SenpaiContext> TO_WALK = new SimpleTransition.Builder<SenpaiContext>()
            .predicate(ctx -> {
                var entity = ctx.getEntity();
                float limbSwingAmount = entity.walkAnimation.speed(ctx.partialTick);
                return (ctx.isMoving() || !(limbSwingAmount > -0.15f && limbSwingAmount < 0.15f)) && !entity.isAggressive();
            })
            .target(WALK)
            .from(RUN, IDLE)
            .build();

    public static final SimpleTransition<SenpaiContext> TO_RUN = new SimpleTransition.Builder<SenpaiContext>()
            .predicate(ctx -> {
                var entity = ctx.getEntity();
                return entity.isAggressive() && ctx.isMoving();
            })
            .target(RUN)
            .from(WALK, IDLE)
            .build();

    public static final SimpleTransition<SenpaiContext> TO_DIE = new SimpleTransition.Builder<SenpaiContext>()
            .predicate(ctx -> ctx.getEntity().isDeadOrDying())
            .target(DIE)
            .from(RUN, WALK, IDLE)
            .build();
}
