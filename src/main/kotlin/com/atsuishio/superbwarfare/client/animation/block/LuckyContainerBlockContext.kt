package com.atsuishio.superbwarfare.client.animation.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.LuckyContainerBlock
import com.atsuishio.superbwarfare.block.entity.LuckyContainerBlockEntity
import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.maydaymemory.mae.basic.DummyPose
import com.maydaymemory.mae.basic.Keyframe
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.runner.AnimationContext
import com.maydaymemory.mae.control.runner.AnimationRunner
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

class LuckyContainerBlockContext(val entity: LuckyContainerBlockEntity) {
    val animations = hashMapOf<String, BedrockAnimation>()
    var partialTick: Float = 0f

    private var animationRunner: AnimationRunner? = null

    init {
        init(ANIM)
    }

    fun init(location: ResourceLocation) {
        val ani = BlockModelReloadListener.getAnimation(location) ?: return
        for (entry in ani) {
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
            entity.level?.playSound(
                null,
                entity.blockPos.x.toDouble(),
                entity.blockPos.y.toDouble(),
                entity.blockPos.z.toDouble(),
                soundEvent,
                SoundSource.BLOCKS,
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

    fun isOpen(): Boolean {
        return entity.blockState.getValue(LuckyContainerBlock.OPENED)
    }

    companion object {
        val ANIM = loc("animations/bedrock/block/lucky_container.animation.json")
    }
}
