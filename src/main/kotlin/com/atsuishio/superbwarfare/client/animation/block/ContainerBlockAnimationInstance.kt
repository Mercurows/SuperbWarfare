package com.atsuishio.superbwarfare.client.animation.block

import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine

class ContainerBlockAnimationInstance(entity: ContainerBlockEntity) {
    val context = ContainerBlockContext(entity)
    private val stateMachine = AnimationStateMachine(ContainerBlockStates.INIT, context) { System.nanoTime() }

    fun tick() {
        stateMachine.tick()
        context.tick()
    }

    fun getPose(): Pose {
        return stateMachine.getPose()
    }
}