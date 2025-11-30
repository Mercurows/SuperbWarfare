package com.atsuishio.superbwarfare.entity

import software.bernie.geckolib.core.animatable.GeoAnimatable
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.core.animation.AnimationController
import software.bernie.geckolib.core.animation.AnimationState
import software.bernie.geckolib.core.animation.RawAnimation
import software.bernie.geckolib.core.`object`.PlayState


class ControllerBuilder<T : GeoAnimatable>(val animatable: T, val data: AnimatableManager.ControllerRegistrar) {
    fun add(
        name: String,
        transitionTickTime: Int = 0,
        animationHandler: AnimationState<T>.() -> PlayState
    ) {
        data.add(AnimationController(animatable, name, transitionTickTime) { it.animationHandler() })
    }

    fun <T : GeoAnimatable> AnimationState<T>.thenPlay(name: String): PlayState =
        setAndContinue(RawAnimation.begin().thenPlay(name))

    fun <T : GeoAnimatable> AnimationState<T>.thenLoop(name: String): PlayState =
        setAndContinue(RawAnimation.begin().thenLoop(name))
}

fun <T : GeoAnimatable> AnimationState<T>.thenPlay(name: String): PlayState =
    setAndContinue(RawAnimation.begin().thenPlay(name))

fun <T : GeoAnimatable> AnimationState<T>.thenLoop(name: String): PlayState =
    setAndContinue(RawAnimation.begin().thenLoop(name))

fun <T : GeoAnimatable> T.buildControllers(
    data: AnimatableManager.ControllerRegistrar,
    builder: ControllerBuilder<T>.() -> Unit
) {
    ControllerBuilder(this, data).apply(builder)
}

