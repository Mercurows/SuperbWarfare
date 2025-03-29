package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Code based on @GoryMoon's Chargers
 */
public record ContainerDataMessage(int containerId, List<Pair> data) implements CustomPacketPayload {
    public static final Type<ContainerDataMessage> TYPE = new Type<>(Mod.loc("container_data"));

    public static final StreamCodec<ByteBuf, ContainerDataMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ContainerDataMessage::containerId,
            StreamCodec.composite(
                    ByteBufCodecs.INT, Pair::id,
                    ByteBufCodecs.INT, Pair::data,
                    Pair::new
            ).apply(ByteBufCodecs.list()),
            ContainerDataMessage::data,
            ContainerDataMessage::new
    );


    public static void handler(ContainerDataMessage message, final IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu.containerId == message.containerId) {
            message.data.forEach(p -> mc.player.containerMenu.setData(p.id, p.data));
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Pair(int id, int data) {
    }

}
