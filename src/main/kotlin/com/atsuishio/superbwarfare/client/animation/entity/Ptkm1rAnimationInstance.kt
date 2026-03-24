package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.projectile.Ptkm1rEntity
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine

class Ptkm1rAnimationInstance(entity: Ptkm1rEntity) {
    val context: BasicEntityContext<Ptkm1rEntity> = BasicEntityContext(entity, BedrockModelLoader.PTKM_1R_MA.second)
    private val stateMachine = AnimationStateMachine(Ptkm1rStates.INIT, context) { System.nanoTime() }

    fun tick() {
        stateMachine.tick()
        context.tick()
    }

    fun getPose(): Pose {
        return stateMachine.getPose()
    }
}