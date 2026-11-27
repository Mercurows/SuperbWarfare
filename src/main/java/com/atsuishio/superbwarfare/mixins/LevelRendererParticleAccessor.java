package com.atsuishio.superbwarfare.mixins;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(LevelRenderer.class)
public interface LevelRendererParticleAccessor {
    @Nullable
    @Invoker("addParticleInternal")
    Particle superbWarfare$addParticle(ParticleOptions options, boolean force, double x, double y, double z,
                                      double vx, double vy, double vz);
}
