package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.menu.ReforgingTableMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record GunReforgeMessage(int msgType) implements CustomPacketPayload {
    public static final Type<GunReforgeMessage> TYPE = new Type<>(Mod.loc("gun_reforge"));

    public static final StreamCodec<ByteBuf, GunReforgeMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            GunReforgeMessage::msgType,
            GunReforgeMessage::new
    );

    public static void handler(GunReforgeMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        AbstractContainerMenu abstractcontainermenu = player.containerMenu;
        if (abstractcontainermenu instanceof ReforgingTableMenu menu) {
            if (!menu.stillValid(player)) {
                return;
            }
            menu.generateResult();
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
