package com.atsuishio.superbwarfare.mixins;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostPass.class)
public class PostPassMixin {

    @Shadow
    @Final
    private EffectInstance effect;

    private static long lastMillis = 0;
    private static float rainbowSeconds = 0;

    /**
     * Sets the RainbowTime uniform to a continuously increasing value in seconds.
     * Unlike Minecraft's built-in Time uniform which wraps every ~1 second,
     * this time source is monotonically increasing with no sudden jumps,
     * allowing arbitrary flow speeds in the handsome goggles rainbow shader.
     */
    @Inject(method = "process(F)V", at = @At("HEAD"))
    private void setRainbowTime(float pPartialTicks, CallbackInfo ci) {
        Uniform uniform = this.effect.getUniform("RainbowTime");
        if (uniform != null) {
            long now = System.currentTimeMillis();
            if (lastMillis == 0) {
                lastMillis = now;
            }
            float delta = (now - lastMillis) / 1000.0f;
            lastMillis = now;
            rainbowSeconds += delta;
            // Prevent float precision loss by keeping the value bounded
            if (rainbowSeconds > 7200.0f) {
                rainbowSeconds -= 3600.0f;
            }
            uniform.set(rainbowSeconds);
        }
    }
}
