package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.entity.vehicle.base.ControllableVehicle;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
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
    public static final Type<VehicleMovementMessage> TYPE = new Type<>(ModUtils.loc("vehicle_movement"));

    public static final StreamCodec<ByteBuf, VehicleMovementMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.SHORT,
            VehicleMovementMessage::keys,
            VehicleMovementMessage::new
    );

    public static void handler(final VehicleMovementMessage message, final IPayloadContext context) {
        var player = (ServerPlayer) context.player();
        var entity = player.getVehicle();
        ItemStack stack = player.getMainHandItem();

        VehicleEntity vehicle = null;
        if (entity instanceof MobileVehicleEntity mobileVehicleEntity) {
            if (mobileVehicleEntity.getFirstPassenger() != player) return;
            vehicle = mobileVehicleEntity;
        } else if (stack.is(ModItems.MONITOR.get())
                && NBTTool.getBoolean(stack, "Using", false)
                && NBTTool.getBoolean(stack, "Linked", false)
        ) vehicle = EntityFindUtil.findDrone(player.level(), NBTTool.getTag(stack).getString("LinkedDrone"));

        if (!(vehicle instanceof ControllableVehicle controllable)) return;
        controllable.processInput(message.keys);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
