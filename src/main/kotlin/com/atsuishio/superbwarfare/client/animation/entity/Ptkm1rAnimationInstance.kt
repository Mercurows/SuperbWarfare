package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.projectile.Ptkm1rEntity
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine

class Ptkm1rAnimationInstance(entity: Ptkm1rEntity) {
    val context = Ptkm1rContext(entity)
    private val stateMachine = AnimationStateMachine(Ptkm1rStates.INIT, context) { System.nanoTime() }

    fun tick() {
        stateMachine.tick()
        context.tick()
    }

    fun getPose(): Pose {
        return stateMachine.getPose()
    }
}