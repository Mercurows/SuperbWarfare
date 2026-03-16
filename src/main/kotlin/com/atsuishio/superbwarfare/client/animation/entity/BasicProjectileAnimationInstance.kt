package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.projectile.BasicGeoProjectileEntity
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine
import net.minecraft.world.entity.Entity

class BasicProjectileAnimationInstance<T>(entity: T) where T : Entity, T : BasicGeoProjectileEntity {
    val context = BasicProjectileContext(entity, entity.animation!!)
    private val stateMachine: AnimationStateMachine<BasicProjectileContext<*>> =
        AnimationStateMachine(BasicProjectileStates.INIT, context) { System.nanoTime() }

    fun tick() {
        context.tick()
        stateMachine.tick()
    }

    fun getPose(): Pose {
        return stateMachine.getPose()
    }
}