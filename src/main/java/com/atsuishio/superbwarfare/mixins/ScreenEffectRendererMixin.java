package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// PJM: при заезде техники в воду камера уходит под воду и ванильный оверлей воды
// (ScreenEffectRenderer.renderFluid -> PoseStack.last()) падает с NoSuchElementException.
// Причина — несбалансированный PoseStack: GameRendererMixin делает pushPose() при езде в
// технике, а popPose() выполняется в другом методе (ClientEventHandler.onRenderHand), из-за
// чего оптимизированный матричный стек Sodium/Iris «underflow»-ит именно на водном эффекте.
// Сидя в закрытой технике «вода на лицо» и так не нужна — отменяем эффект целиком, устраняя краш.
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {

    @Inject(method = "renderScreenEffect", at = @At("HEAD"), cancellable = true)
    private static void superbWarfare$skipInVehicle(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        Entity camera = minecraft.getCameraEntity();
        if (camera != null && camera.getRootVehicle() instanceof VehicleEntity) {
            ci.cancel();
        }
    }
}
