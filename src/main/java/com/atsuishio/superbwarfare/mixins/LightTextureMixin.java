package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class LightTextureMixin {

    @Shadow
    @Final
    private NativeImage lightPixels;

    @Inject(method = "updateLightTexture",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"))
    private void forceMaxSkyBrightness(CallbackInfo ci) {
        if (ClientEventHandler.hasThermalImagingGoggles()) {
            var light = 15;
            if (ClientEventHandler.activeThermalImaging) {
                light = 6;
            }

            for (int blockLight = 0; blockLight < 16; blockLight++) {
                int brightPixel = this.lightPixels.getPixelRGBA(light, blockLight);
                for (int skyLight = 0; skyLight < 15; skyLight++) {
                    this.lightPixels.setPixelRGBA(skyLight, blockLight, brightPixel);
                }
            }
        }
    }
}
