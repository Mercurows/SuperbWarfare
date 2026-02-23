package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Author: MrCrayfish
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @ModifyVariable(method = "turnPlayer(D)V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 1)
    private double sensitivity(double original) {
        return ClientMouseHandler.INSTANCE.changeSensitivity(original);
    }

    @Unique
    private static double sbw121$x;
    @Unique
    private static double sbw121$y;

    @ModifyVariable(method = "turnPlayer(D)V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 4)
    private double modifyD0(double d) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return d;
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return d;

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            sbw121$x = d;

            double i = 0;

            if (vehicle.getRoll() < 0) {
                i = 1;
            } else if (vehicle.getRoll() > 0) {
                i = -1;
            }

            if (Mth.abs(vehicle.getRoll()) > 90) {
                i *= (1 - (Mth.abs(vehicle.getRoll()) - 90) / 90);
            }

            return (1 - (Mth.abs(vehicle.getRoll()) / 90)) * d + ((Mth.abs(vehicle.getRoll()) / 90)) * sbw121$y * i;
        }
        return d;
    }

    @ModifyVariable(method = "turnPlayer(D)V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 5)
    private double modifyD1(double d) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return d;
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return d;

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            sbw121$y = d;
            return (1 - (Mth.abs(vehicle.getRoll()) / 90)) * d + ((Mth.abs(vehicle.getRoll()) / 90)) * sbw121$x * (vehicle.getRoll() < 0 ? -1 : 1);
        }

        return d;
    }

}
