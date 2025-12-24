package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // 感谢 Minecraft-Ping-Wheel 开源
    // https://github.com/LukenSkyne/Minecraft-Ping-Wheel/blob/138295954dab9d2451ad19e16d8d413ef018a2d8/fabric/src/main/java/nx/pingwheel/fabric/mixin/LevelRendererMixin.java
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;applyModelViewMatrix()V", ordinal = 1, shift = At.Shift.AFTER))
    private void onStartRenderLevel(PoseStack $$0, float tickDelta, long $$2, boolean $$3, Camera $$4, GameRenderer $$5, LightTexture $$6, Matrix4f $$7, CallbackInfo ci) {
        VectorUtil.modelViewMatrix = RenderSystem.getModelViewMatrix();
        VectorUtil.projectionMatrix = RenderSystem.getProjectionMatrix();
    }
}
