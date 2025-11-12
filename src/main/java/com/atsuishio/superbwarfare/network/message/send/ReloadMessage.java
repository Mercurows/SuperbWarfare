package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public enum ReloadMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                pressAction(context.getSender());
            }
        });
        context.setPacketHandled(true);
    }

    public static void pressAction(Player player) {
        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            var gunData = vehicle.getGunData(vehicle.getSeatIndex(player));
            if (gunData != null && gunData.countBackupAmmo(vehicle) > 0 && (gunData.compute().autoLoadWhileEmpty)) {
                gunData.vehicleReload.set(true);
                return;
            }
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        GunEventHandler.tryStartReload(player, GunData.from(stack));
    }
}
