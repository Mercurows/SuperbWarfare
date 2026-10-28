package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.event.ClickEventHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventoryMixin {

    @Shadow
    @Final
    public Player player;

    /**
     * On vehicles equipped with weapons, switching the quickbar (e.g., LB/RB on a controller)
     * will now switch the vehicle's weapons.
     */
    @Inject(method = "swapPaint(D)V", at = @At("HEAD"), cancellable = true)
    private void swapPaint(double direction, CallbackInfo ci) {
        if (ClickEventHandler.INSTANCE.switchVehicleWeapon(player, direction)) ci.cancel();
    }
}