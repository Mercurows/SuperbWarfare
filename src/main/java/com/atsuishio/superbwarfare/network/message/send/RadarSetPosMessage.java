package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record RadarSetPosMessage(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RadarSetPosMessage> TYPE = new Type<>(Mod.loc("radar_set_pos"));

    public static final StreamCodec<ByteBuf, RadarSetPosMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            RadarSetPosMessage::pos,
            RadarSetPosMessage::new
    );


    public static void handler(RadarSetPosMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        AbstractContainerMenu menu = player.containerMenu;
        if (menu instanceof FuMO25Menu fuMO25Menu) {
            if (!player.containerMenu.stillValid(player)) return;
            fuMO25Menu.setPos(message.pos.getX(), message.pos.getY(), message.pos.getZ());
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
