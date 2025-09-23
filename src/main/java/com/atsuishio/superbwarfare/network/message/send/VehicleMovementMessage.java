package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record VehicleMovementMessage(short keys) implements CustomPacketPayload {
    public static final Type<VehicleMovementMessage> TYPE = new Type<>(Mod.loc("vehicle_movement"));

    public static final StreamCodec<ByteBuf, VehicleMovementMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.SHORT,
            VehicleMovementMessage::keys,
            VehicleMovementMessage::new
    );

    public static void handler(final VehicleMovementMessage message, final IPayloadContext context) {
        var player = (ServerPlayer) context.player();
        var entity = player.getVehicle();
        ItemStack stack = player.getMainHandItem();
        final var tag = NBTTool.getTag(stack);

        VehicleEntity vehicle = null;
        if (entity instanceof VehicleEntity vehicleEntity && vehicleEntity.getFirstPassenger() == player) {
            vehicle = vehicleEntity;
        } else if (stack.is(ModItems.MONITOR.get())
                && tag.getBoolean("Using")
                && tag.getBoolean("Linked")
        ) vehicle = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));

        if (vehicle == null) return;
        vehicle.processInput(message.keys);
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
