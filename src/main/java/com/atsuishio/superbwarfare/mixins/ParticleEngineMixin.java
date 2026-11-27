package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.renderer.special.ExplosionParticleRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    private static final String RENDER = "render(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;"
            + "Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V";

    // Forge's Frustum overload keeps its name in production.
    @Inject(method = RENDER, at = @At("HEAD"), remap = false)
    private void superbWarfare$beginParticlePass(CallbackInfo ci) {
        ExplosionParticleRenderer.beginParticlePass();
    }

    @WrapOperation(method = RENDER, remap = false, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
    private void superbWarfare$deferExplosionParticle(Particle particle, VertexConsumer vertices, Camera camera,
                                                   float partialTick, Operation<Void> original) {
        if (!ExplosionParticleRenderer.defer(particle, camera, partialTick)) {
            original.call(particle, vertices, camera, partialTick);
        }
    }
}
