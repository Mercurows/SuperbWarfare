package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class LightTextureMixin {

    @Shadow
    @Final
    private NativeImage lightPixels;

    @ModifyVariable(method = "updateLightTexture", at = @At("STORE"), ordinal = 2)
    private float modifyNightVisionF5(float f5) {
        if (ClientEventHandler.hasThermalImagingGoggles()) {
            if (ClientEventHandler.activeThermalImaging) {
                return 0.51F;
            } else {
                return 1.0F;
            }
        }

        var player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof VehicleEntity vehicle) {
            var index = vehicle.getSeatIndex(player);
            var seats = vehicle.computed().seats();
            if (index >= 0 && index < seats.size()) {
                var seat = seats.get(index);
                if (seat.hasThermalImaging && ClientEventHandler.activeThermalImaging) {
                    return 0.51F;
                }
            }
        }

        return f5;
    }

    @Inject(method = "updateLightTexture",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"))
    private void forceMaxSkyBrightness(CallbackInfo ci) {
        if (!superbwarfare$needsBrightnessBoost()) return;

        // Copy sky=15 (max) column to all other sky columns to force full brightness everywhere
        for (int blockLight = 0; blockLight < 16; blockLight++) {
            int brightPixel = this.lightPixels.getPixelRGBA(15, blockLight);
            for (int skyLight = 0; skyLight < 15; skyLight++) {
                this.lightPixels.setPixelRGBA(skyLight, blockLight, brightPixel);
            }
        }
    }

    @Unique
    private static boolean superbwarfare$needsBrightnessBoost() {
        // Thermal imaging goggles (active or just worn)
        if (ClientEventHandler.hasThermalImagingGoggles()) {
            return true;
        }

        // Vehicle seat with thermal imaging
        var player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof VehicleEntity vehicle) {
            var index = vehicle.getSeatIndex(player);
            var seats = vehicle.computed().seats();
            if (index >= 0 && index < seats.size()) {
                return seats.get(index).hasThermalImaging && ClientEventHandler.activeThermalImaging;
            }
        }

        return false;
    }
}
