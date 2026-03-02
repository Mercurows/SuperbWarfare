package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.BedrockModelLoader
import com.atsuishio.superbwarfare.client.BedrockModelLoader.getAnimations
import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.entity.SenpaiEntity
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.maydaymemory.mae.basic.Keyframe
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.runner.AnimationContext
import com.maydaymemory.mae.control.runner.AnimationRunner
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import kotlin.math.abs

class SenpaiContext(@JvmField var entity: SenpaiEntity) {
    var animations = hashMapOf<String, BedrockAnimation>()

    @JvmField
    var partialTick: Float = 0f

    private var animationRunner: AnimationRunner? = null

    init {
        val ani = getAnimations(BedrockModelLoader.SENPAI_ANI)
        for (entry in ani!!) {
            animations[entry.name] = entry
        }
    }

    fun isRunner(): Boolean {
        return entity.runner
    }

    fun isMoving(): Boolean {
        val velocity = entity.deltaMovement
        val avgVelocity = (abs(velocity.x) + abs(velocity.z)).toFloat() / 2f
        return avgVelocity > 0.015f
    }

    fun tick() {
        if (animationRunner != null) {
            animationRunner!!.tick()
            val namedSounds = animationRunner!!.clip<ResourceLocation>(BedrockAnimation.SOUND_CHANNEL_NAME)
            if (namedSounds != null) {
                processSounds(namedSounds)
            }
        }
    }

    fun processSounds(sounds: Iterable<Keyframe<ResourceLocation>>) {
        for (keyframe in sounds) {
            val soundLocation = keyframe.getValue()
            val soundEvent = SoundEvent.createVariableRangeEvent(soundLocation)
            entity.level().playSound(
                null,
                entity.x,
                entity.y,
                entity.z,
                soundEvent,
                entity.soundSource,
                1.0f,
                1.0f
            )
        }
    }

    fun playAnimation(animationName: String?, type: AnimationPlayType) {
        val animation = animations[animationName]
        if (animation != null) {
            animationRunner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
            animationRunner!!.state = type.state()
        }
    }

    fun getPose(): Pose {
        return animationRunner!!.evaluate()
    }
}
