package com.atsuishio.superbwarfare.client.animation.item

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.HandedBedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.DummyPose
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionf

class TaserItemAnimationInstance(
    val entity: LocalPlayer,
    var stack: ItemStack,
    val animations: Map<String, BedrockAnimation>,
    val model: HandedBedrockModel
) : IFPAnimationInstance {
    companion object {
        private val BLENDER: EulerAdditiveBlender =
            SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }

    private val context: TaserContext = TaserContext(entity, this.animations)
    private val animationStateMachine: AnimationStateMachine<TaserContext> =
        AnimationStateMachine(TaserStates.INIT, this.context) { System.nanoTime() }
    private var rotation: Quaternionf = Quaternionf()
    private var poseCache: Pose = DummyPose.INSTANCE
    private var draw = false

    override fun currentItem(): ItemStack = this.stack

    override fun getPose(): Pose? {
        return this.animationStateMachine.pose
    }

    override fun tick(tick: Float) {
        this.animationStateMachine.tick()
        this.poseCache = BLENDER.blend(this.model.pose, this.pose)
    }

    // TODO 什么逆天视角
    override fun getCameraRotation(): Quaternionf {
        return this.rotation
    }

    override fun setCameraRotation(rotation: Quaternionf) {
        this.rotation = rotation
    }

    override fun getCachedPose(): Pose {
        return this.poseCache
    }

    override fun updateItem(itemStack: ItemStack) {
        this.stack = itemStack
    }

    override fun triggerDraw() {
        if (!this.draw) {
            this.draw = true
            TaserStates.INIT.onEnter(this.context, TaserStates.INIT)
        }
    }

    override fun triggerPutAway() {
        this.context.ended = true
    }
}