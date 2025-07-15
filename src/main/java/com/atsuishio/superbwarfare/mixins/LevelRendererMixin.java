package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
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
    // https://github.com/LukenSkyne/Minecraft-Ping-Wheel/blob/ede72b18f57bd9dfe55ef44afe61190421fbc084/common/src/main/java/nx/pingwheel/common/mixin/LevelRendererMixin.java

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;applyModelViewMatrix()V", ordinal = 0, shift = At.Shift.AFTER))
    private void onStartRenderLevel(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        VectorUtil.modelViewMatrix = RenderSystem.getModelViewMatrix();
        VectorUtil.projectionMatrix = RenderSystem.getProjectionMatrix();
    }
}
