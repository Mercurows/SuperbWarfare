package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.screens.FuMO25ScreenHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record RadarMenuOpenMessage(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RadarMenuOpenMessage> TYPE = new Type<>(Mod.loc("radar_menu_open"));

    public static final StreamCodec<ByteBuf, RadarMenuOpenMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            RadarMenuOpenMessage::pos,
            RadarMenuOpenMessage::new
    );

    public static void handler(RadarMenuOpenMessage message, final IPayloadContext context) {
        FuMO25ScreenHelper.resetEntities();
        FuMO25ScreenHelper.pos = message.pos;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
