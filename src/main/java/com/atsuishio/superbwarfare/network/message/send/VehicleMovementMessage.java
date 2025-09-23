package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ItemNBTTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record VehicleMovementMessage(short keys) {

    public static VehicleMovementMessage decode(FriendlyByteBuf buffer) {
        return new VehicleMovementMessage(buffer.readShort());
    }

    public static void encode(VehicleMovementMessage message, FriendlyByteBuf buffer) {
        buffer.writeShort(message.keys);
    }

    public static void handler(VehicleMovementMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                var player = (ServerPlayer) context.getSender();
                var entity = player.getVehicle();
                ItemStack stack = player.getMainHandItem();

                VehicleEntity vehicle = null;
                if (entity instanceof VehicleEntity vehicleEntity && vehicleEntity.getFirstPassenger() == player) {
                    vehicle = vehicleEntity;
                } else if (stack.is(ModItems.MONITOR.get())
                        && ItemNBTTool.getBoolean(stack, "Using", false)
                        && ItemNBTTool.getBoolean(stack, "Linked", false)
                ) vehicle = EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone"));

                if (vehicle == null) return;
                vehicle.processInput(message.keys);
            }
        });
        context.setPacketHandled(true);
    }
}
