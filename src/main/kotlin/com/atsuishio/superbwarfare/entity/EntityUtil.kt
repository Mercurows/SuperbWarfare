package com.atsuishio.superbwarfare.entity

import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.PlayState
import software.bernie.geckolib.animation.RawAnimation

class ControllerBuilder<T : GeoAnimatable>(val animatable: T, val data: ControllerRegistrar) {
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
    data: ControllerRegistrar,
    builder: ControllerBuilder<T>.() -> Unit
) {
    ControllerBuilder(this, data).apply(builder)
}

