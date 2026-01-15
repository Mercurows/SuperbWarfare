package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class LightTextureMixin {

    @Inject(method = "getDarknessGamma(F)F",
            at = @At("RETURN"), cancellable = true)
    private void getDarknessGamma(float pPartialTick, CallbackInfoReturnable<Float> cir) {
        if (ClientEventHandler.activeThermalImaging) {
            cir.cancel();
            cir.setReturnValue(8f);
        }
    }

    @Inject(method = "calculateDarknessScale(Lnet/minecraft/world/entity/LivingEntity;FF)F",
            at = @At("RETURN"), cancellable = true)
    private void calculateDarknessScale(LivingEntity pEntity, float pGamma, float pPartialTick, CallbackInfoReturnable<Float> cir) {
        if (ClientEventHandler.activeThermalImaging) {
            cir.cancel();
            cir.setReturnValue(0.25f);
        }
    }
}
