package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.resource.model.VehicleModelReloadListener
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.DummyPose
import com.maydaymemory.mae.basic.Keyframe
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.maydaymemory.mae.control.runner.AnimationContext
import com.maydaymemory.mae.control.runner.AnimationRunner
import com.maydaymemory.mae.control.runner.StopState
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.Entity

class VehicleAnimationContext<T>(val entity: T, location: ResourceLocation) where T : Entity, T : BasicGeoVehicleEntity {
    val animations = hashMapOf<String, BedrockAnimation>()
    var partialTick: Float = 0f

    private val runners = mutableListOf<AnimationRunner>()

    init {
        val ani = VehicleModelReloadListener.getAnimation(location)
        if (ani != null) {
            for (entry in ani) {
                animations[entry.name] = entry
            }
        }
    }

    fun playAnimation(animationName: String?, type: AnimationPlayType) {
        val animation = animations[animationName] ?: return
        val runner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
        runner.state = type.state()
        runners.add(runner)
    }

    fun tick() {
        val iterator = runners.iterator()
        while (iterator.hasNext()) {
            val runner = iterator.next()
            runner.tick()
            if (runner.state is StopState) {
                iterator.remove()
            }
        }
        for (runner in runners) {
            val namedSounds = runner.clip<ResourceLocation>(BedrockAnimation.SOUND_CHANNEL_NAME)
            if (namedSounds != null) {
                processSounds(namedSounds)
            }
        }
    }

    private fun processSounds(sounds: Iterable<Keyframe<ResourceLocation>>) {
        for (keyframe in sounds) {
            val soundLocation = keyframe.getValue()
            val soundEvent = SoundEvent.createVariableRangeEvent(soundLocation)
            entity.level().playSound(
                null, entity.x, entity.y, entity.z,
                soundEvent, entity.soundSource, 1.0f, 1.0f
            )
        }
    }

    fun getPose(): Pose {
        if (runners.isEmpty()) return DummyPose.INSTANCE
        var result: Pose = DummyPose.INSTANCE
        for (runner in runners) {
            val pose = runner.evaluate()
            if (pose != DummyPose.INSTANCE) {
                result = if (result == DummyPose.INSTANCE) pose else BLENDER.blend(result, pose)
            }
        }
        return result
    }

    companion object {
        val BLENDER: EulerAdditiveBlender =
            SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}
