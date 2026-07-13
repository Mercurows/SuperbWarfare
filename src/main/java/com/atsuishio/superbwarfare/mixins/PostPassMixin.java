package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.shader.ThermalShaderHandler;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostPass.class)
public class PostPassMixin {

    @Shadow
    @Final
    private EffectInstance effect;

    @Unique
    private static long superbwarfare$lastMillis = 0;

    @Unique
    private static float superbwarfare$rainbowSeconds = 0;

    /**
     * Sets the RainbowTime uniform to a continuously increasing value in seconds.
     * Unlike Minecraft's built-in Time uniform which wraps every ~1 second,
     * this time source is monotonically increasing with no sudden jumps,
     * allowing arbitrary flow speeds in the handsome goggles rainbow shader.
     */
    @Inject(method = "process(F)V", at = @At("HEAD"))
    private void setRainbowTime(float pPartialTicks, CallbackInfo ci) {
        // Advance the shared monotonic clock on every pass.  delta is wall-clock based, so the
        // total stays correct no matter how many passes call this per frame.  Must run even when
        // RainbowTime is absent, otherwise the thermal shader's ThermalTime stays frozen at 0
        // whenever the rainbow goggles aren't worn -> its sensor noise looks static. // PJM
        long now = System.currentTimeMillis();
        if (superbwarfare$lastMillis == 0) {
            superbwarfare$lastMillis = now;
        }
        float delta = (now - superbwarfare$lastMillis) / 1000.0f;
        superbwarfare$lastMillis = now;
        superbwarfare$rainbowSeconds += delta;
        // Prevent float precision loss by keeping the value bounded
        if (superbwarfare$rainbowSeconds > 7200.0f) {
            superbwarfare$rainbowSeconds -= 3600.0f;
        }

        Uniform uniform = this.effect.getUniform("RainbowTime");
        if (uniform != null) {
            uniform.set(superbwarfare$rainbowSeconds);
        }

        Uniform thermalTime = this.effect.getUniform("ThermalTime");
        if (thermalTime != null) {
            thermalTime.set(superbwarfare$rainbowSeconds);
        }

        Uniform thermalArtifactStrength = this.effect.getUniform("ThermalArtifactStrength");
        if (thermalArtifactStrength != null) {
            thermalArtifactStrength.set(ThermalShaderHandler.isVehicleMode() ? 1.0f : 0.0f);
        }

        Uniform thermalInterference = this.effect.getUniform("ThermalInterference");
        if (thermalInterference != null) {
            thermalInterference.set(ThermalShaderHandler.getInterference());
        }
    }
}
