package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    // 感谢 Minecraft-Ping-Wheel 开源
    // https://github.com/LukenSkyne/Minecraft-Ping-Wheel/blob/138295954dab9d2451ad19e16d8d413ef018a2d8/fabric/src/main/java/nx/pingwheel/fabric/mixin/LevelRendererMixin.java
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;applyModelViewMatrix()V", ordinal = 1, shift = At.Shift.AFTER))
    private void onStartRenderLevel(PoseStack $$0, float tickDelta, long $$2, boolean $$3, Camera $$4, GameRenderer $$5, LightTexture $$6, Matrix4f $$7, CallbackInfo ci) {
        ClientEventHandler.modelViewMatrix = RenderSystem.getModelViewMatrix();
        ClientEventHandler.projectionMatrix = RenderSystem.getProjectionMatrix();
    }

    @Inject(method = "renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD"), cancellable = true)
    private void renderEntity(Entity pEntity, double pCamX, double pCamY, double pCamZ, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, CallbackInfo ci) {
        // 这里只改实体的亮度
        if (ClientEventHandler.activeThermalImaging) {
            ci.cancel();
            double d0 = Mth.lerp(pPartialTick, pEntity.xOld, pEntity.getX());
            double d1 = Mth.lerp(pPartialTick, pEntity.yOld, pEntity.getY());
            double d2 = Mth.lerp(pPartialTick, pEntity.zOld, pEntity.getZ());
            float f = Mth.lerp(pPartialTick, pEntity.yRotO, pEntity.getYRot());
            this.entityRenderDispatcher.render(pEntity, d0 - pCamX, d1 - pCamY, d2 - pCamZ, f, pPartialTick, pPoseStack,
                    pBufferSource, LightTexture.FULL_BRIGHT);
        }
    }
}
