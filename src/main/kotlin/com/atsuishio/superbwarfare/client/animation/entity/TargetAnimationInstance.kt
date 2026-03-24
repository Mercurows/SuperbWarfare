package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine

class TargetAnimationInstance(entity: TargetEntity) {
    val context: TargetContext = TargetContext(entity)
    private val stateMachine = AnimationStateMachine(TargetStates.INIT, context) { System.nanoTime() }

    fun tick() {
        stateMachine.tick()
        context.tick()
    }

    fun getPose(): Pose {
        return stateMachine.getPose()
    }
}