package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.SenpaiEntity
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine

class SenpaiAnimationInstance(entity: SenpaiEntity) {
    val context: SenpaiContext = SenpaiContext(entity)
    private val stateMachine = AnimationStateMachine(SenpaiStates.IDLE, context) { System.nanoTime() }

    init {
        SenpaiStates.IDLE.onEnter(this.context, SenpaiStates.IDLE)
    }

    fun tick() {
        stateMachine.tick()
        context.tick()
    }

    fun getPose(): Pose {
        return stateMachine.getPose()
    }
}
