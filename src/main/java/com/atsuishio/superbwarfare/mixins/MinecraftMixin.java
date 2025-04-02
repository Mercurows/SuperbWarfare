package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.network.message.send.ChangeVehicleSeatMessage;
import com.atsuishio.superbwarfare.network.message.send.SwitchVehicleWeaponMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Final
    public Options options;

    /**
     * 在可切换座位的载具上，按下潜行键+数字键时切换座位
     * 在有武器的载具上，按下数字键时切换武器
     */
    @Inject(method = "handleKeybinds()V", at = @At("HEAD"), cancellable = true)
    private void handleKeybinds(CallbackInfo ci) {
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) return;

        var index = -1;
        for (int i = 0; i < 9; ++i) {
            if (options.keyHotbarSlots[i].isDown()) {
                index = i;
                break;
            }
        }
        if (index == -1) return;

        // shift+数字键 座位更改
        if (vehicle.getMaxPassengers() > 1
                && options.keyShift.isDown()
                && index < vehicle.getMaxPassengers()
                && vehicle.getNthEntity(index) == null
        ) {
            ci.cancel();
            options.keyHotbarSlots[index].consumeClick();

            PacketDistributor.sendToServer(new ChangeVehicleSeatMessage(index));
            vehicle.changeSeat(player, index);

            return;
        }

        var seatIndex = vehicle.getSeatIndex(player);

        if (vehicle instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.banHand(player)) {
            ci.cancel();
            options.keyHotbarSlots[index].consumeClick();

            // 数字键 武器切换
            if (!options.keyShift.isDown()
                    && weaponVehicle.hasWeapon(seatIndex)
                    && weaponVehicle.getWeaponIndex(seatIndex) != index) {
                PacketDistributor.sendToServer(new SwitchVehicleWeaponMessage(seatIndex, index, false));
            }
        }
    }
}
