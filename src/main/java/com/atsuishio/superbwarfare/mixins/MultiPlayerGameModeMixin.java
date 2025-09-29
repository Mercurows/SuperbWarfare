package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "isServerControlledInventory", at = @At("HEAD"), cancellable = true)
    public void isServerControlledInventory(CallbackInfoReturnable<Boolean> cir) {
        var player = this.minecraft.player;
        if (player == null) return;
        if (player.isPassenger() && player.getVehicle() instanceof VehicleEntity vehicle) {
            cir.setReturnValue(vehicle.hasMenu());
        }
    }
}
