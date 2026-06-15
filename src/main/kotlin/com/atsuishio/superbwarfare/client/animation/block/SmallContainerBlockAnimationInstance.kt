package com.atsuishio.superbwarfare.client.animation.block

import com.atsuishio.superbwarfare.block.entity.SmallContainerBlockEntity
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine

class SmallContainerBlockAnimationInstance(entity: SmallContainerBlockEntity) {
    val context = SmallContainerBlockContext(entity)
    private val stateMachine = AnimationStateMachine(SmallContainerBlockStates.INIT, context) { System.nanoTime() }

    fun tick() {
        stateMachine.tick()
        context.tick()
    }

    fun getPose(): Pose {
        return stateMachine.getPose()
    }
}
