package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class KeymappingMixin {

    @Shadow
    private InputConstants.Key key;

    @Shadow
    private int clickCount;

    @Inject(method = "consumeClick()Z", at = @At("HEAD"), cancellable = true)
    public void consumeClick(CallbackInfoReturnable<Boolean> cir) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) return;

        for (int i = 0; i < 9; i++) {
            if (Minecraft.getInstance().options.keyHotbarSlots[i].getKey() == key) {
                if (vehicle.getMaxPassengers() > 1
                        && Screen.hasShiftDown()
                        && i < vehicle.getMaxPassengers()
                        && vehicle.getNthEntity(i) == null
                ) {
                    if (this.clickCount > 0) {
                        --this.clickCount;
                    }
                    cir.setReturnValue(false);
                }

                if (vehicle instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.banHand(player)) {
                    if (this.clickCount > 0) {
                        --this.clickCount;
                    }
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
