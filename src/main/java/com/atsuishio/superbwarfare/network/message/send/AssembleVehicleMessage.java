package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.menu.VehicleAssemblingMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record AssembleVehicleMessage(ResourceLocation id, int containerId) implements CustomPacketPayload {
    public static final Type<AssembleVehicleMessage> TYPE = new Type<>(Mod.loc("assemble_vehicle"));

    public static final StreamCodec<ByteBuf, AssembleVehicleMessage> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, AssembleVehicleMessage::id,
            ByteBufCodecs.INT, AssembleVehicleMessage::containerId,
            AssembleVehicleMessage::new
    );

    public static void handler(AssembleVehicleMessage message, final IPayloadContext context) {
        var player = context.player();
        if (player.containerMenu.containerId != message.containerId) return;
        if (player.containerMenu instanceof VehicleAssemblingMenu menu) {
            menu.assembleVehicle(message.id, player);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
