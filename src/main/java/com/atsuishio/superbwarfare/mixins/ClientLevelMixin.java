package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"), cancellable = true)
    public void getSkyColor(Vec3 pPos, float pPartialTick, CallbackInfoReturnable<Vec3> cir) {
        if (ClientEventHandler.activeThermalImaging) {
            cir.cancel();
            cir.setReturnValue(new Vec3(0.1, 0.1, 0.1));
        }
    }

    @Inject(method = "getCloudColor(F)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"), cancellable = true)
    public void getCloudColor(float pPartialTick, CallbackInfoReturnable<Vec3> cir) {
        if (ClientEventHandler.activeThermalImaging) {
            cir.cancel();
            cir.setReturnValue(new Vec3(0.2, 0.2, 0.2));
        }
    }
}
