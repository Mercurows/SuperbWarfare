package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.tools.GunsTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public record GunsDataMessage(HashMap<String, HashMap<String, Double>> gunsData) implements CustomPacketPayload {
    public static final Type<GunsDataMessage> TYPE = new Type<>(Mod.loc("set_guns_data"));


    public static final StreamCodec<ByteBuf, GunsDataMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(
                    HashMap::new,
                    ByteBufCodecs.STRING_UTF8,
                    ByteBufCodecs.map(
                            HashMap::new,
                            ByteBufCodecs.STRING_UTF8,
                            ByteBufCodecs.DOUBLE
                    )
            ),
            GunsDataMessage::gunsData,
            GunsDataMessage::new
    );


    public static void handler(final GunsDataMessage message, final IPayloadContext context) {
        GunsTool.gunsData = message.gunsData;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
