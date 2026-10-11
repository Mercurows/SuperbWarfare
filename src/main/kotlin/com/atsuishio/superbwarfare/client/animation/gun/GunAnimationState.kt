package com.atsuishio.superbwarfare.client.animation.gun

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType

enum class GunAnimationState(val playType: AnimationPlayType) {
    IDLE(AnimationPlayType.LOOP),
    EDIT(AnimationPlayType.PLAY_ONCE_HOLD),
    BOLT(AnimationPlayType.PLAY_ONCE_HOLD),
    RELOAD(AnimationPlayType.PLAY_ONCE_HOLD),
    RELOAD_NORMAL(AnimationPlayType.PLAY_ONCE_HOLD),
    RELOAD_EMPTY(AnimationPlayType.PLAY_ONCE_HOLD),
    PREPARE(AnimationPlayType.PLAY_ONCE_HOLD),
    PREPARE_LOAD(AnimationPlayType.PLAY_ONCE_HOLD),
    ITERATIVE(AnimationPlayType.LOOP),
    ITERATIVE_2(AnimationPlayType.LOOP),
    FINISH(AnimationPlayType.PLAY_ONCE_HOLD),
    MELEE(AnimationPlayType.PLAY_ONCE_HOLD),
    FIRE(AnimationPlayType.PLAY_ONCE_STOP),
    CHARGE(AnimationPlayType.PLAY_ONCE_HOLD);

    val isReload: Boolean
        get() = this == RELOAD ||
                this == RELOAD_NORMAL ||
                this == RELOAD_EMPTY ||
                this == PREPARE ||
                this == PREPARE_LOAD ||
                this == ITERATIVE ||
                this == ITERATIVE_2 ||
                this == FINISH

    /**
     * 这一状态下主武器的动画会自己决定手待在哪
     */
    val takesHandAway: Boolean
        get() = isReload || this == MELEE || this == EDIT
}
