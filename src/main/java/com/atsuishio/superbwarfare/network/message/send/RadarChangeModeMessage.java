package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record RadarChangeModeMessage(byte mode) implements CustomPacketPayload {
    public static final Type<RadarChangeModeMessage> TYPE = new Type<>(Mod.loc("radar_change_mode"));

    public static final StreamCodec<ByteBuf, RadarChangeModeMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE,
            RadarChangeModeMessage::mode,
            RadarChangeModeMessage::new
    );

    public static void handler(RadarChangeModeMessage message, final IPayloadContext context) {
        byte mode = message.mode;
        if (mode < 1 || mode > 4) return;

        ServerPlayer player = (ServerPlayer) context.player();

        AbstractContainerMenu menu = player.containerMenu;
        if (menu instanceof FuMO25Menu fuMO25Menu) {
            if (!player.containerMenu.stillValid(player)) return;
            fuMO25Menu.setFuncTypeAndTime(mode);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
