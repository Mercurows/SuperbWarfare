package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

@Mixin(GeoRenderer.class)
public abstract class GeoRendererMixin<T extends GeoAnimatable>{

    @Shadow public abstract GeoModel<T> getGeoModel();

    @Inject(method = "getRenderType(Lsoftware/bernie/geckolib/core/animatable/GeoAnimatable;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/MultiBufferSource;F)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("RETURN"), cancellable = true, remap = false)
    public void getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick, CallbackInfoReturnable<RenderType> cir) {
        if (ClientEventHandler.activeThermalImaging && ClientEventHandler.thermalImagingMode == 0) {
            cir.cancel();
            cir.setReturnValue(getGeoModel().getRenderType(animatable, texture));
        }
    }
}
