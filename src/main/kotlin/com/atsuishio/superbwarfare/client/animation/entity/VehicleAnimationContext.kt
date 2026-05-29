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
import org.joml.Vector3f

class VehicleAnimationContext<T>(val entity: T, location: ResourceLocation) where T : Entity, T : BasicGeoVehicleEntity {
    val animations = hashMapOf<String, BedrockAnimation>()
    var partialTick: Float = 0f

    private val weaponRunners = linkedMapOf<String, AnimationRunner>()
    private val weaponIndices = hashMapOf<String, Int>()

    private val fadeMap = hashMapOf<String, FadeInfo>()

    companion object {
        val BLENDER: EulerAdditiveBlender =
            SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }

        const val DEFAULT_FADE_TICKS = 10
    }

    private data class FadeInfo(
        val weaponName: String,
        val isFadingIn: Boolean,
        val fadeTicks: Int,
        var elapsed: Int = 0
    ) {
        fun alpha(): Float {
            val progress = elapsed.toFloat() / fadeTicks.toFloat()
            return if (isFadingIn) {
                progress.coerceIn(0f, 1f)
            } else {
                (1f - progress).coerceIn(0f, 1f)
            }
        }

        fun isDone(): Boolean = elapsed >= fadeTicks
    }

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

    fun playAnimation(animationName: String?, type: AnimationPlayType, fadeInTicks: Int = 0) {
        val animation = animations[animationName] ?: return
        val runner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
        runner.state = type.state()

        val weaponName = extractWeaponName(animationName)
        if (weaponName != null) {
            fadeMap.remove(weaponName)
            weaponRunners[weaponName] = runner
            if (fadeInTicks > 0) {
                fadeMap[weaponName] = FadeInfo(weaponName, true, fadeInTicks)
            }
        }
    }

    fun stopAnimation(animationName: String, fadeOutTicks: Int = 0) {
        val weaponName = extractWeaponName(animationName) ?: return
        if (fadeOutTicks > 0 && weaponRunners.containsKey(weaponName)) {
            fadeMap[weaponName] = FadeInfo(weaponName, false, fadeOutTicks)
        } else {
            weaponRunners.remove(weaponName)
            weaponIndices.remove(weaponName)
            fadeMap.remove(weaponName)
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
        // Process fade transitions
        val completedFades = mutableListOf<String>()
        for ((weaponName, fade) in fadeMap) {
            fade.elapsed++
            if (fade.isDone()) {
                completedFades.add(weaponName)
            }
        }
        for (weaponName in completedFades) {
            val fade = fadeMap.remove(weaponName) ?: continue
            if (!fade.isFadingIn) {
                // Fade-out complete: remove the runner and transition to idle if available
                val index = weaponIndices[weaponName]
                weaponRunners.remove(weaponName)
                weaponIndices.remove(weaponName)
                val base = weaponName.substringBeforeLast('#')
                val idleExists = if (index != null && index > 0) {
                    animations.containsKey("animation.$base.idle.$index") ||
                        animations.containsKey("animation.$base.idle")
                } else {
                    animations.containsKey("animation.$base.idle")
                }
                if (idleExists) {
                    startIdle(weaponName)
                }
            }
        }

        val transitionToIdle = mutableListOf<String>()
        val toRemove = mutableListOf<String>()

        for ((weaponName, runner) in weaponRunners) {
            // Skip ticking for fading-out runners that aren't being processed
            if (fadeMap[weaponName]?.isFadingIn == false) continue
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
        for ((weaponName, runner) in weaponRunners) {
            var pose = runner.evaluate()
            if (pose != DummyPose.INSTANCE) {
                val alpha = fadeMap[weaponName]?.alpha() ?: 1f
                if (alpha < 1f) {
                    pose = scalePose(pose, alpha)
                }
                result = if (result == DummyPose.INSTANCE) pose else BLENDER.blend(result, pose)
            }
        }
        return result
    }

    private fun scalePose(pose: Pose, alpha: Float): Pose {
        if (alpha >= 1f) return pose
        if (alpha <= 0f) return DummyPose.INSTANCE

        val builder = ArrayPoseBuilder()
        for (transform in pose.getBoneTransforms()) {
            // Translation: scale toward [0,0,0] (additive)
            val translation = Vector3f(transform.translation()).mul(alpha)

            // Rotation (Euler angles): scale toward [0,0,0] (additive)
            val euler = Vector3f(transform.rotation().asEulerAngle())
            euler.mul(alpha)

            // Scale: lerp from [1,1,1] toward target (multiplicative)
            val origScale = transform.scale()
            val newScale = Vector3f(
                1f + (origScale.x() - 1f) * alpha,
                1f + (origScale.y() - 1f) * alpha,
                1f + (origScale.z() - 1f) * alpha
            )

            builder.addBoneTransform(
                BoneTransform(transform.boneIndex(), translation, ZYXRotationView(Vector3f(euler)), newScale)
            )
        }
        return builder.toPose()
    }
}
