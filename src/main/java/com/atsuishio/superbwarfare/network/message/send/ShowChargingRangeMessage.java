package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.menu.ChargingStationMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ShowChargingRangeMessage(boolean operation) implements CustomPacketPayload {
    public static final Type<ShowChargingRangeMessage> TYPE = new Type<>(Mod.loc("show_charging_range"));

    public static final StreamCodec<ByteBuf, ShowChargingRangeMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ShowChargingRangeMessage::operation,
            ShowChargingRangeMessage::new
    );


    public static void handler(ShowChargingRangeMessage message, final IPayloadContext context) {
        var player = context.player();
        var menu = player.containerMenu;
        if (menu instanceof ChargingStationMenu chargingStationMenu) {
            if (!chargingStationMenu.stillValid(player)) return;

            chargingStationMenu.setShowRange(message.operation);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
