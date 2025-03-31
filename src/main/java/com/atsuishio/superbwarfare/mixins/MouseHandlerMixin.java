package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.config.client.VehicleControlConfig;
import com.atsuishio.superbwarfare.entity.vehicle.*;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.GunsTool;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.droneFovLerp;
import static com.atsuishio.superbwarfare.event.ClientEventHandler.isFreeCam;

/**
 * Author: MrCrayfish
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    private static double x;
    private static double y;

    @ModifyVariable(method = "turnPlayer()V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 2)
    private double sensitivity(double original) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return original;

        if (player.hasEffect(ModMobEffects.SHOCK.get()) && !player.isSpectator()) {
            return 0;
        }

        ItemStack stack = mc.player.getMainHandItem();

        if (isFreeCam(player)) {
            return 0;
        }

        if (player.getVehicle() instanceof CannonEntity) {
            return ClientEventHandler.zoomVehicle ? 0.15 : 0.3;
        }

        if (player.getVehicle() instanceof Lav150Entity) {
            return ClientEventHandler.zoomVehicle ? 0.23 : 0.3;
        }

        if (player.getVehicle() instanceof Bmp2Entity) {
            return ClientEventHandler.zoomVehicle ? 0.22 : 0.27;
        }

        if (player.getVehicle() instanceof Yx100Entity yx100) {
            if (player == yx100.getFirstPassenger()) {
                return ClientEventHandler.zoomVehicle ? 0.17 : 0.22;
            } else if (player ==yx100.getNthEntity(1)){
                return ClientEventHandler.zoomVehicle ? 0.25 : 0.35;
            }
        }

        if (player.getVehicle() instanceof PrismTankEntity) {
            return ClientEventHandler.zoomVehicle ? 0.26 : 0.33;
        }

        if (player.getVehicle() instanceof Ah6Entity ah6Entity && !ah6Entity.onGround() && ah6Entity.getFirstPassenger() == player) {
            return 0.33;
        }

        if (player.getVehicle() instanceof Tom6Entity) {
            return 0.3;
        }

        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
            return 0.33 / (1 + 0.08 * (droneFovLerp - 1));
        }

        if (!stack.is(ModTags.Items.GUN)) {
            return original;
        }

        double zoom = 1.25 + GunsTool.getGunDoubleTag(stack, "CustomZoom", 0);
        float customSens = (float) stack.getOrCreateTag().getInt("sensitivity");

        if (!player.getMainHandItem().isEmpty() && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            return original / Math.max((1 + (0.2 * (zoom - (0.3 * customSens)) * ClientEventHandler.zoomTime)), 0.1);
        }

        return original;
    }

    @ModifyVariable(method = "turnPlayer()V", at = @At(value = "STORE", opcode = Opcodes.ISTORE))
    private int modifyI(int i) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return i;

        if (player.getVehicle() instanceof Ah6Entity ah6Entity && ah6Entity.getFirstPassenger() == player) {
            return VehicleControlConfig.INVERT_AIRCRAFT_CONTROL.get() ? -i : i;
        }

        if (player.getVehicle() instanceof Tom6Entity tom6 && tom6.getFirstPassenger() == player) {
            return VehicleControlConfig.INVERT_AIRCRAFT_CONTROL.get() ? -i : i;
        }
        return i;
    }

    @ModifyVariable(method = "turnPlayer()V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 5)
    private double modifyD2(double d) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return d;

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            x = d;

            double i = 0;

            if (vehicle.getRoll() < 0) {
                i = 1;
            } else if (vehicle.getRoll() > 0) {
                i = -1;
            }

            if (Mth.abs(vehicle.getRoll()) > 90) {
                i *= (1 - (Mth.abs(vehicle.getRoll()) - 90) / 90);
            }

            return -(1 - (Mth.abs(vehicle.getRoll()) / 90)) * d + ((Mth.abs(vehicle.getRoll()) / 90)) * y * i;
        }
        return d;
    }

    @ModifyVariable(method = "turnPlayer()V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 6)
    private double modifyD3(double d) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return d;

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            y = d;
            return -(1 - (Mth.abs(vehicle.getRoll()) / 90)) * d + ((Mth.abs(vehicle.getRoll()) / 90)) * x * (vehicle.getRoll() < 0 ? -1 : 1);
        }

        return d;
    }

}
