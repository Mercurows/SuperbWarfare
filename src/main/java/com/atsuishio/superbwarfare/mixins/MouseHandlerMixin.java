package com.atsuishio.superbwarfare.mixins;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Author: MrCrayfish
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    private static double x;
    private static double y;

    // TODO what are these???
//    @ModifyVariable(method = "turnPlayer()V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 5)
//    private double modifyD2(double d) {
//        Minecraft mc = Minecraft.getInstance();
//        Player player = mc.player;
//
//        if (player == null) return d;
//        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return d;
//
//        if (player.getVehicle() instanceof VehicleEntity vehicle) {
//            x = d;
//
//            double i = 0;
//
//            if (vehicle.getRoll() < 0) {
//                i = 1;
//            } else if (vehicle.getRoll() > 0) {
//                i = -1;
//            }
//
//            if (Mth.abs(vehicle.getRoll()) > 90) {
//                i *= (1 - (Mth.abs(vehicle.getRoll()) - 90) / 90);
//            }
//
//            return (1 - (Mth.abs(vehicle.getRoll()) / 90)) * d + ((Mth.abs(vehicle.getRoll()) / 90)) * y * i;
//        }
//        return d;
//    }
//
//    @ModifyVariable(method = "turnPlayer()V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 6)
//    private double modifyD3(double d) {
//        Minecraft mc = Minecraft.getInstance();
//        Player player = mc.player;
//
//        if (player == null) return d;
//        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return d;
//
//        if (player.getVehicle() instanceof VehicleEntity vehicle) {
//            y = d;
//            return (1 - (Mth.abs(vehicle.getRoll()) / 90)) * d + ((Mth.abs(vehicle.getRoll()) / 90)) * x * (vehicle.getRoll() < 0 ? -1 : 1);
//        }
//
//        return d;
//    }

}
