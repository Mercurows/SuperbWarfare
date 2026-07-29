package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.lighting.LightPositionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects dynamic light levels from {@link LightPositionRegistry} into the
 * vanilla light engine read path, ensuring dynamic levels are returned at
 * any altitude (including empty-air chunk sections where no {@code DataLayer} exists).
 *
 * @author paralax034
 * @since 0.8.9.1
 */
@Mixin(LightEngine.class)
public abstract class LightEngineMixin {

    /**
     * Intercepts the light read path at {@link At.Shift#BY} return point.
     *
     * @param pos the world-space block position being queried
     * @param cir mutable return value callback; overwritten only when a live spark
     *            exists and its computed intensity exceeds the vanilla result
     */
    @Inject(method = "getLightValue(Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private void sbw$injectDynamicLightRead(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        // Fast-path guard: restrict injection exclusively to block light engine instances
        if (!((Object) this instanceof BlockLightEngine)) return;
        // Fast-path guard: zero CPU overhead when no dynamic sparks are currently active
        if (LightPositionRegistry.isEmpty()) return;

        // O(1) primitive hashmap lookup by packed Long coordinate
        int sparkLevel = LightPositionRegistry.getLevel(pos.asLong());
        if (sparkLevel <= 0) return;

        // Override vanilla value only if dynamic spark intensity is strictly higher
        if (cir.getReturnValueI() < sparkLevel) {
            cir.setReturnValue(sparkLevel);
        }
    }
}