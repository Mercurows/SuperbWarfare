package com.atsuishio.superbwarfare.client.animation.block

import com.atsuishio.superbwarfare.block.entity.LuckyContainerBlockEntity
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine

class LuckyContainerBlockAnimationInstance(entity: LuckyContainerBlockEntity) {
    val context = LuckyContainerBlockContext(entity)
    private val stateMachine = AnimationStateMachine(LuckyContainerBlockStates.INIT, context) { System.nanoTime() }

    fun tick() {
        stateMachine.tick()
        context.tick()
    }

    fun getPose(): Pose {
        return stateMachine.getPose()
    }
}
