package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.lighting.GunMuzzleLightHandler;
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeThermalRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Both vanilla and animated gun arms use these entry points; held items do not. */
@Mixin(PlayerRenderer.class)
public class PlayerHandThermalMixin {
    @ModifyVariable(method = {"renderRightHand", "renderLeftHand"}, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int superbWarfare$lightMuzzleArm(int packedLight) {
        return GunMuzzleLightHandler.firstPersonLight(packedLight);
    }

    @ModifyVariable(method = {"renderRightHand", "renderLeftHand"}, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private MultiBufferSource superbWarfare$captureThermalArm(MultiBufferSource source) {
        return ScopeThermalRenderer.captureFirstPersonArm(source);
    }
}
