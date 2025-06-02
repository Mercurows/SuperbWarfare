package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record MouseMoveMessage(double speedX, double speedY) implements CustomPacketPayload {
    public static final Type<MouseMoveMessage> TYPE = new Type<>(Mod.loc("mouse_move"));

    public static final StreamCodec<ByteBuf, MouseMoveMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            MouseMoveMessage::speedX,
            ByteBufCodecs.DOUBLE,
            MouseMoveMessage::speedY,
            MouseMoveMessage::new
    );

    public static void handler(MouseMoveMessage message, final IPayloadContext context) {
        var player = context.player();
        var entity = player.getVehicle();

        if (entity instanceof VehicleEntity vehicle) {
            vehicle.mouseInput(message.speedX, message.speedY);
        }

        ItemStack stack = player.getMainHandItem();
        var tag = NBTTool.getTag(stack);

        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            DroneEntity drone = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));
            if (drone != null) {
                drone.mouseInput(message.speedX, message.speedY);
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
