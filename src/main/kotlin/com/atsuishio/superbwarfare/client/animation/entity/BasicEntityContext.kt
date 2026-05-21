package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.resource.model.EntityModelReloadListener
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.maydaymemory.mae.basic.DummyPose
import com.maydaymemory.mae.basic.Keyframe
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.runner.AnimationContext
import com.maydaymemory.mae.control.runner.AnimationRunner
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.Entity

open class BasicEntityContext<T : Entity>(val entity: T, location: ResourceLocation) {
    val animations = hashMapOf<String, BedrockAnimation>()
    var partialTick: Float = 0f

    private var animationRunner: AnimationRunner? = null

    init {
        init(location)
    }

    open fun init(location: ResourceLocation) {
        val ani = EntityModelReloadListener.getAnimation(location)
        for (entry in ani!!) {
            animations[entry.name] = entry
        }
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
        return animationRunner?.evaluate() ?: DummyPose.INSTANCE
    }
}