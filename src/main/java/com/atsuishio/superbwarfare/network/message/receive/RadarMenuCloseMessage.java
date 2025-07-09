package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.screens.FuMO25ScreenHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public enum RadarMenuCloseMessage implements CustomPacketPayload {
    INSTANCE;

    public static final Type<RadarMenuCloseMessage> TYPE = new Type<>(Mod.loc("radar_menu_close"));

    public static final StreamCodec<ByteBuf, RadarMenuCloseMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handler() {
        FuMO25ScreenHelper.resetEntities();
        FuMO25ScreenHelper.pos = null;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
