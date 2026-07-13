package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
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
    // https://github.com/LukenSkyne/Minecraft-Ping-Wheel/blob/ede72b18f57bd9dfe55ef44afe61190421fbc084/common/src/main/java/nx/pingwheel/common/mixin/LevelRendererMixin.java

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;applyModelViewMatrix()V", ordinal = 0, shift = At.Shift.AFTER))
    private void onStartRenderLevel(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        ClientEventHandler.modelViewMatrix = RenderSystem.getModelViewMatrix();
        ClientEventHandler.projectionMatrix = RenderSystem.getProjectionMatrix();
    }

    // TODO 找到真正把实体渲染在世界中的位置进行mixin
    @Inject(method = "renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD"), cancellable = true)
    private void renderEntity(Entity pEntity, double pCamX, double pCamY, double pCamZ, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, CallbackInfo ci) {
        // 这里只改实体的亮度
        // PJM: старый full-bright ре-рендер сущностей во время тепловизора отключён — его заменил
        // ThermalShaderHandler (отдельный thermal_buffer / ThermalSampler). Дублирующий рендер той
        // же сущности в основной буфер с другим partialTick давал «призрак» с рассинхроном позы
        // (дёрганье голов) и перекрывал анимацию смерти. Оставляем ванильный рендер (ci не отменяем).
    }
}
