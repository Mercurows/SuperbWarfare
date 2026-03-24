package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.living.SenpaiEntity
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine

class SenpaiAnimationInstance(entity: SenpaiEntity) {
    val context: SenpaiContext = SenpaiContext(entity)
    private val stateMachine = AnimationStateMachine(SenpaiStates.INIT, context) { System.nanoTime() }

    fun tick() {
        stateMachine.tick()
        context.tick()
    }

    fun getPose(): Pose {
        return stateMachine.getPose()
    }
}
