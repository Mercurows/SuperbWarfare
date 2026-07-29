package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.lighting.LightPositionRegistry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects dynamic light emission levels into {@link BlockLightEngine} during
 * light propagation ticks, driving vanilla light spread onto nearby terrain blocks.
 *
 * @author paralax034
 * @since 0.8.9.1
 */
@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin {

    /**
     * Intercepts block light emission during the propagation pass (BFS light engine update).
     *
     * @param packedPos world-space position packed as a 64-bit Long (x, y, z)
     * @param state     the block state at the queried position
     * @param cir       mutable return value callback
     */
    @Inject(method = "getEmission(JLnet/minecraft/world/level/block/state/BlockState;)I", at = @At("HEAD"), cancellable = true)
    private void sbw$injectDynamicEmission(long packedPos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        int level = LightPositionRegistry.getLevel(packedPos);
        if (level > 0) {
            cir.setReturnValue(level);
        }
    }
}