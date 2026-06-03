/*
 * Ported from Raise Sound Limit Simplified (Unofficial Forge Port)
 * https://github.com/RelativityMC/raise-sound-limit-simplified
 *
 * Original authors: RelativityMC, ishland, mrqx0195
 * Licensed under the MIT License.
 *
 * Thanks to the original authors for their work on raising Minecraft's sound source limit.
 */
package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.sound.SoundLimit;
import com.mojang.blaze3d.audio.Library;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modifies the maximum sound source limits in {@link Library#init(String, boolean)}.
 * <p>
 * Minecraft by default caps static sound sources at 255 and streaming sources at 8.
 * This mixin redirects the {@link Mth#clamp} calls to use higher limits.
 */
@Mixin(Library.class)
public class LibraryMixin {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Verify the mixin is being applied at all.
     */
    @Inject(method = "init(Ljava/lang/String;Z)V", at = @At("HEAD"))
    private void onInitHead(String deviceSpecifier, boolean enableHrtf, CallbackInfo ci) {
        LOGGER.info("LibraryMixin: init called, will patch sound limits (static={}, streaming={})",
            SoundLimit.maxSourcesCount, SoundLimit.maxStreamingSources);
    }

    /**
     * Redirect the FIRST Mth.clamp call (ordinal 0) — the streaming source limit.
     * Original: {@code Mth.clamp(sqrt(channelCount), 2, 8)}
     * Patched: uses maxStreamingSources as the upper bound.
     */
    @Redirect(
        method = "init(Ljava/lang/String;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;clamp(III)I",
            ordinal = 0
        )
    )
    private int redirectStreamingClamp(int value, int min, int max) {
        return Mth.clamp(value, min, SoundLimit.maxStreamingSources);
    }

    /**
     * Redirect the SECOND Mth.clamp call (ordinal 1) — the static source limit.
     * Original: {@code Mth.clamp(channelCount - streamingCount, 8, 255)}
     * Patched: uses maxSourcesCount as the upper bound.
     */
    @Redirect(
        method = "init(Ljava/lang/String;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;clamp(III)I",
            ordinal = 1
        )
    )
    private int redirectStaticClamp(int value, int min, int max) {
        return Mth.clamp(value, min, SoundLimit.maxSourcesCount);
    }
}
