package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine

class DPSGeneratorAnimationInstance(entity: DPSGeneratorEntity) {
    val context: DPSGeneratorContext = DPSGeneratorContext(entity)
    private val stateMachine = AnimationStateMachine(DPSGeneratorStates.INIT, context) { System.nanoTime() }

    fun tick() {
        stateMachine.tick()
        context.tick()
    }

    fun getPose(): Pose {
        return stateMachine.getPose()
    }
}