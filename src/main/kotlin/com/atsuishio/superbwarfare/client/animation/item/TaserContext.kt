package com.atsuishio.superbwarfare.client.animation.item

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.maydaymemory.mae.basic.DummyPose
import com.maydaymemory.mae.basic.Keyframe
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.Tickable
import com.maydaymemory.mae.control.runner.AnimationContext
import com.maydaymemory.mae.control.runner.AnimationRunner
import net.minecraft.client.player.LocalPlayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent

class TaserContext(private val entity: LocalPlayer, private val animations: Map<String, BedrockAnimation>) : Tickable {
    private var animationRunner: AnimationRunner? = null
    var initialized = false
    var ended = false

    override fun tick() {
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
        return animationRunner?.evaluate() ?: DummyPose.INSTANCE
    }
}