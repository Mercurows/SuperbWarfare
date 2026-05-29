package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.resource.model.VehicleModelReloadListener
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.maydaymemory.mae.basic.*
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
    private val weaponIndices = hashMapOf<String, Int>()

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
        // 处理无编号的 idle 动画：animation.X.idle
        for ((name, animation) in animations) {
            if (name.startsWith("animation.") && name.endsWith(".idle")) {
                val rest = name.removePrefix("animation.").removeSuffix(".idle")
                val runner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
                runner.state = AnimationPlayType.LOOP.state()
                weaponRunners[rest] = runner
            }
        }

        // 处理带序号的 idle 动画: animation.X.idle.1, animation.X.idle.2, etc.
        val idlePattern = Regex("^animation\\.(.+)\\.idle\\.(\\d+)$")
        for ((name, animation) in animations) {
            val match = idlePattern.matchEntire(name) ?: continue
            val weaponName = match.groupValues[1]
            val index = match.groupValues[2].toInt()
            val key = "$weaponName#$index"
            val runner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
            runner.state = AnimationPlayType.LOOP.state()
            weaponRunners[key] = runner
            weaponIndices[key] = index
        }
    }

    fun fire(weaponName: String, index: Int) {
        val key: String
        val fireAnimName: String
        if (index == 0) {
            fireAnimName = "animation.$weaponName.fire"
            key = weaponName
        } else {
            key = "$weaponName#$index"
            val specificName = "animation.$weaponName.fire.$index"
            fireAnimName = if (animations.containsKey(specificName)) {
                specificName
            } else {
                "animation.$weaponName.fire"
            }
        }

        val fireAnimation = animations[fireAnimName] ?: return

        val runner = AnimationRunner(fireAnimation, AnimationContext(fireAnimation.specifiedEndTimeS))
        runner.state = AnimationPlayType.PLAY_ONCE_STOP.state()
        weaponRunners[key] = runner
        if (index != 0) {
            weaponIndices[key] = index
        }
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

    fun stopAnimation(animationName: String) {
        val weaponName = extractWeaponName(animationName) ?: return
        weaponRunners.remove(weaponName)
        weaponIndices.remove(weaponName)
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
                val index = weaponIndices[weaponName]
                val base = weaponName.substringBeforeLast('#')
                val idleExists = if (index != null && index > 0) {
                    animations.containsKey("animation.$base.idle.$index") ||
                        animations.containsKey("animation.$base.idle")
                } else {
                    animations.containsKey("animation.$base.idle")
                }
                if (idleExists) {
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
        val index = weaponIndices[weaponName]
        val idleAnimName: String

        if (index != null && index > 0) {
            val base = weaponName.substringBeforeLast('#')
            val specificName = "animation.$base.idle.$index"
            if (animations.containsKey(specificName)) {
                idleAnimName = specificName
            } else {
                idleAnimName = "animation.$base.idle"
            }
        } else {
            val base = weaponName.substringBeforeLast('#')
            idleAnimName = "animation.$base.idle"
        }

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
