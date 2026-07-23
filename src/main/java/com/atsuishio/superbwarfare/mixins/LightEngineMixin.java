package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.lighting.LightPositionRegistry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects dynamic light levels from {@link LightPositionRegistry} into the
 * vanilla block light engine, enabling temporary animated lights without
 * placing real light-emitting blocks.
 */
@Mixin(BlockLightEngine.class)
public abstract class LightEngineMixin {

    @Inject(method = "getEmission(JLnet/minecraft/world/level/block/state/BlockState;)I", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbw$injectDynamicLight(long packedPos, BlockState state,
                                        CallbackInfoReturnable<Integer> cir) {
        int level = LightPositionRegistry.getLevel(packedPos);
        if (level > 0) {
            cir.setReturnValue(level);
        }
    }
}