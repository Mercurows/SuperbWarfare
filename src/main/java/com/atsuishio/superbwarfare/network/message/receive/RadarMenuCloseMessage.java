package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.screens.FuMO25ScreenHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record RadarMenuCloseMessage(int msgType) implements CustomPacketPayload {
    public static final Type<RadarMenuCloseMessage> TYPE = new Type<>(Mod.loc("radar_menu_close"));

    public static final StreamCodec<ByteBuf, RadarMenuCloseMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            RadarMenuCloseMessage::msgType,
            RadarMenuCloseMessage::new
    );

    public static void handler(RadarMenuCloseMessage message, final IPayloadContext context) {
        FuMO25ScreenHelper.resetEntities();
        FuMO25ScreenHelper.pos = null;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
