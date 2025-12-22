package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // 感谢 Minecraft-Ping-Wheel 开源
    // https://github.com/LukenSkyne/Minecraft-Ping-Wheel/blob/ede72b18f57bd9dfe55ef44afe61190421fbc084/common/src/main/java/nx/pingwheel/common/mixin/LevelRendererMixin.java

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;applyModelViewMatrix()V", ordinal = 0, shift = At.Shift.AFTER))
    private void onStartRenderLevel(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        VectorUtil.modelViewMatrix = RenderSystem.getModelViewMatrix();
        VectorUtil.projectionMatrix = RenderSystem.getProjectionMatrix();
    }

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    //TODO 正确实现mixin

    @ModifyVariable(
            method = "renderEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
                    shift = At.Shift.AFTER
            ),
            index = 7, // MultiBufferSource 参数的索引
            argsOnly = true
    )
    private MultiBufferSource modifyBufferSource(
            MultiBufferSource originalBuffer,
            Entity entity,
            double camX, double camY, double camZ,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource // 这是原始的 bufferSource
    ) {

        return renderType -> bufferSource.getBuffer(RenderType.endPortal());
    }
}
