package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.config.client.ControlConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.AirEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isFreeCam;

/**
 * Author: MrCrayfish
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Unique
    private static double sbw$x;
    @Unique
    private static double sbw$y;

    @ModifyVariable(method = "turnPlayer()V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 2)
    private double sensitivity(double original) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return original;

        if (player.hasEffect(ModMobEffects.SHOCK.get()) && !player.isSpectator()) {
            return 0;
        }

        ItemStack stack = mc.player.getMainHandItem();

        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
            return 0;
        }

        if (isFreeCam(player)) {
            return 0;
        }

        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.banHand(player)) {
            return vehicle.getSensitivity(original, ClientEventHandler.zoomVehicle, vehicle.getSeatIndex(player), vehicle.onGround());
        }

        if (stack.getItem() instanceof GunItem) {
            var data = GunData.from(stack);
            float customSens = data.sensitivity.get();

            if (!player.getMainHandItem().isEmpty() && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                return original / Math.max((1 + (0.2 * (data.zoom() - (0.3 * customSens)) * ClientEventHandler.zoomTime)), 0.1) * (ControlConfig.MOUSE_SENSITIVITY.get() / 100f);
            }
        }

        return original;
    }

    @ModifyVariable(method = "turnPlayer()V", at = @At(value = "STORE", opcode = Opcodes.ISTORE))
    private int modifyI(int i) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        // 反转鼠标
        if (player == null) return i;

        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle instanceof AirEntity && vehicle.getFirstPassenger() == player) {
            return ControlConfig.INVERT_AIRCRAFT_CONTROL.get() ? -i : i;
        }
        return i;
    }

    @ModifyVariable(method = "turnPlayer()V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 5)
    private double modifyD2(double d) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return d;
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return d;

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            sbw$x = d;

            double i = 0;

            if (vehicle.getRoll() < 0) {
                i = 1;
            } else if (vehicle.getRoll() > 0) {
                i = -1;
            }

            if (Mth.abs(vehicle.getRoll()) > 90) {
                i *= (1 - (Mth.abs(vehicle.getRoll()) - 90) / 90);
            }

            return (1 - (Mth.abs(vehicle.getRoll()) / 90)) * d + ((Mth.abs(vehicle.getRoll()) / 90)) * sbw$y * i;
        }
        return d;
    }

    @ModifyVariable(method = "turnPlayer()V", at = @At(value = "STORE", opcode = Opcodes.DSTORE), ordinal = 6)
    private double modifyD3(double d) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return d;
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return d;

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            sbw$y = d;
            return (1 - (Mth.abs(vehicle.getRoll()) / 90)) * d + ((Mth.abs(vehicle.getRoll()) / 90)) * sbw$x * (vehicle.getRoll() < 0 ? -1 : 1);
        }

        return d;
    }
}
