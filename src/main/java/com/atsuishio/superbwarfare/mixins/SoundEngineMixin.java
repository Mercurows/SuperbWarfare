package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.overlay.FlashbangOverlay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    // The initial play path calls the float/category overload rather than the instance overload.
    @WrapOperation(method = "play", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float superbWarfare$flashbangNewSound(SoundEngine engine, float volume, SoundSource source,
                                                 Operation<Float> original, SoundInstance sound) {
        return original.call(engine, volume, source) * FlashbangOverlay.soundVolumeMultiplier(sound);
    }

    // Used by tickable sounds and by category refreshes for existing non-tickable sounds.
    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN"), cancellable = true)
    private void superbWarfare$flashbangActiveSound(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValueF() * FlashbangOverlay.soundVolumeMultiplier(sound));
    }
}
