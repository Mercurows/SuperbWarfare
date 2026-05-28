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

    private val weaponRunners = linkedMapOf<String, AnimationRunner>()

    init {
        val ani = VehicleModelReloadListener.getAnimation(location)
        if (ani != null) {
            for (entry in ani) {
                animations[entry.name] = entry
            }
        }
        startIdleAnimations()
    }

    private fun startIdleAnimations() {
        for ((name, animation) in animations) {
            if (name.startsWith("animation.") && name.endsWith(".idle")) {
                val weaponName = name.removePrefix("animation.").removeSuffix(".idle")
                val runner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
                runner.state = AnimationPlayType.LOOP.state()
                weaponRunners[weaponName] = runner
            }
        }
    }

    fun fire(weaponName: String) {
        val fireAnimName = "animation.$weaponName.fire"
        val fireAnimation = animations[fireAnimName] ?: return

        val runner = AnimationRunner(fireAnimation, AnimationContext(fireAnimation.specifiedEndTimeS))
        runner.state = AnimationPlayType.PLAY_ONCE_STOP.state()
        weaponRunners[weaponName] = runner
    }

    fun playAnimation(animationName: String?, type: AnimationPlayType) {
        val animation = animations[animationName] ?: return
        val runner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
        runner.state = type.state()

        val weaponName = extractWeaponName(animationName)
        if (weaponName != null) {
            weaponRunners[weaponName] = runner
        }
    }

    private fun extractWeaponName(animationName: String?): String? {
        if (animationName == null) return null
        val name = animationName.removePrefix("animation.")
        val dotIndex = name.lastIndexOf('.')
        if (dotIndex <= 0) return null
        return name.substring(0, dotIndex)
    }

    fun tick() {
        val transitionToIdle = mutableListOf<String>()
        val toRemove = mutableListOf<String>()

        for ((weaponName, runner) in weaponRunners) {
            runner.tick()
            if (runner.state is StopState) {
                val idleAnimName = "animation.$weaponName.idle"
                if (animations.containsKey(idleAnimName)) {
                    transitionToIdle.add(weaponName)
                } else {
                    toRemove.add(weaponName)
                }
            }
        }

        for (weaponName in transitionToIdle) {
            startIdle(weaponName)
        }
        for (weaponName in toRemove) {
            weaponRunners.remove(weaponName)
        }

        for (runner in weaponRunners.values) {
            val namedSounds = runner.clip<ResourceLocation>(BedrockAnimation.SOUND_CHANNEL_NAME)
            if (namedSounds != null) {
                processSounds(namedSounds)
            }
        }
    }

    private fun startIdle(weaponName: String) {
        val idleAnimName = "animation.$weaponName.idle"
        val idleAnimation = animations[idleAnimName] ?: return

        val runner = AnimationRunner(idleAnimation, AnimationContext(idleAnimation.specifiedEndTimeS))
        runner.state = AnimationPlayType.LOOP.state()
        weaponRunners[weaponName] = runner
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
        if (weaponRunners.isEmpty()) return DummyPose.INSTANCE
        var result: Pose = DummyPose.INSTANCE
        for (runner in weaponRunners.values) {
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
