package com.atsuishio.superbwarfare.mixins;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Author: MrCrayfish
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

//    @Unique
//    private static double sbw121$x;
//    @Unique
//    private static double sbw121$y;

    // TODO 正确实现视角计算
//    @ModifyVariable(method = "turnPlayer(D)V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 3)
//    private double modifyD0(double d) {
//        Minecraft mc = Minecraft.getInstance();
//        Player player = mc.player;
//
//        if (player == null) return d;
//        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return d;
//
//        if (player.getVehicle() instanceof VehicleEntity vehicle) {
//            sbw121$x = d;
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
//            return (1 - (Mth.abs(vehicle.getRoll()) / 90)) * d + ((Mth.abs(vehicle.getRoll()) / 90)) * sbw121$y * i;
//        }
//        return d;
//    }
//
//    @ModifyVariable(method = "turnPlayer(D)V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 4)
//    private double modifyD1(double d) {
//        Minecraft mc = Minecraft.getInstance();
//        Player player = mc.player;
//
//        if (player == null) return d;
//        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return d;
//
//        if (player.getVehicle() instanceof VehicleEntity vehicle) {
//            sbw121$y = d;
//            return (1 - (Mth.abs(vehicle.getRoll()) / 90)) * d + ((Mth.abs(vehicle.getRoll()) / 90)) * sbw121$x * (vehicle.getRoll() < 0 ? -1 : 1);
//        }
//
//        return d;
//    }

}
