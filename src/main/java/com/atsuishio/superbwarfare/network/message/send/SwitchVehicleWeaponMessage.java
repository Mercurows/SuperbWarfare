package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SwitchVehicleWeaponMessage {

    private final int index;
    private final double value;
    private final boolean isScroll;

    public SwitchVehicleWeaponMessage(int index, double value, boolean isScroll) {
        this.index = index;
        this.value = value;
        this.isScroll = isScroll;
    }

    public static void encode(SwitchVehicleWeaponMessage message, FriendlyByteBuf byteBuf) {
        byteBuf.writeInt(message.index);
        byteBuf.writeDouble(message.value);
        byteBuf.writeBoolean(message.isScroll);
    }

    public static SwitchVehicleWeaponMessage decode(FriendlyByteBuf byteBuf) {
        return new SwitchVehicleWeaponMessage(byteBuf.readInt(), byteBuf.readDouble(), byteBuf.readBoolean());
    }

    public static void handler(SwitchVehicleWeaponMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }

            if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
                var value = message.isScroll ? (Mth.clamp(message.value > 0 ? Mth.ceil(message.value) : Mth.floor(message.value), -1, 1)) : message.value;
                vehicle.changeWeapon(message.index, (int) value, message.isScroll);
            }
        });
        context.get().setPacketHandled(true);
    }
}
