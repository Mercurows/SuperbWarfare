package com.atsuishio.superbwarfare.client.renderer.animations;

import com.atsuishio.superbwarfare.entity.SenpaiEntity;
import com.atsuishio.superbwarfare.resource.BedrockModelLoader;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Keyframe;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SenpaiContext {
    public Map<String, BedrockAnimation> animations = new HashMap<>();
    public SenpaiEntity entity;
    public float partialTick = 0f;

    public SenpaiContext(SenpaiEntity entity) {
        this.entity = entity;
        var ani = BedrockModelLoader.getAnimations(BedrockModelLoader.SENPAI_ANI);
        for (var entry : ani) {
            animations.put(entry.getName(), entry);
        }
    }

    private AnimationRunner animationRunner;

    public boolean isRunner() {
        return entity.getRunner();
    }

    public SenpaiEntity getEntity() {
        return entity;
    }

    public boolean isMoving() {
        Vec3 velocity = entity.getDeltaMovement();
        float avgVelocity = (float)(Math.abs(velocity.x) + Math.abs(velocity.z)) / 2f;
        return  avgVelocity > 0.015f;
    }

    public void tick() {
        if (animationRunner != null) {
            animationRunner.tick();
            Iterable<Keyframe<ResourceLocation>> namedSounds = animationRunner.clip(BedrockAnimation.SOUND_CHANNEL_NAME);
            if (namedSounds != null) {
                processSounds(namedSounds);
            }
        }
    }

    public float getPartialTick() {
        return partialTick;
    }

    public void setPartialTick(float partialTick) {
        this.partialTick = partialTick;
    }

    public void processSounds(Iterable<Keyframe<ResourceLocation>> sounds) {
        for (Keyframe<ResourceLocation> keyframe : sounds) {
            ResourceLocation soundLocation = keyframe.getValue();
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundLocation);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), soundEvent, entity.getSoundSource(), 1.0F, 1.0F);
        }
    }

    public void playAnimation(String animationName, @NotNull AnimationPlayType type) {
        BedrockAnimation animation = animations.get(animationName);
        if (animation != null) {
            animationRunner = new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS()));
            animationRunner.setState(type.state());
        }
    }

    public Pose getPose() {
        if (animations == null) {
            return DummyPose.INSTANCE;
        }
        return animationRunner.evaluate();
    }
}
