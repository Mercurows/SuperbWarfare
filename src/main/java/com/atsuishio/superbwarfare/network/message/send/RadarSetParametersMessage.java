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

public record RadarSetParametersMessage(int mode) implements CustomPacketPayload {
    public static final Type<RadarSetParametersMessage> TYPE = new Type<>(Mod.loc("radar_set_parameters"));

    public static final StreamCodec<ByteBuf, RadarSetParametersMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            RadarSetParametersMessage::mode,
            RadarSetParametersMessage::new
    );

    public static void handler(RadarSetParametersMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        AbstractContainerMenu menu = player.containerMenu;
        if (menu instanceof FuMO25Menu fuMO25Menu) {
            if (!player.containerMenu.stillValid(player)) {
                return;
            }
            fuMO25Menu.setPosToParameters();
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
