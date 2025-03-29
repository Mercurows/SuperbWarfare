package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.menu.ReforgingTableMenu;
import com.atsuishio.superbwarfare.perk.Perk;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetPerkLevelMessage(int msgType, boolean add) implements CustomPacketPayload {
    public static final Type<SetPerkLevelMessage> TYPE = new Type<>(Mod.loc("set_perk_level"));

    public static final StreamCodec<ByteBuf, SetPerkLevelMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SetPerkLevelMessage::msgType,
            ByteBufCodecs.BOOL,
            SetPerkLevelMessage::add,
            SetPerkLevelMessage::new
    );

    public static void handler(SetPerkLevelMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        AbstractContainerMenu abstractcontainermenu = player.containerMenu;

        if (abstractcontainermenu instanceof ReforgingTableMenu menu) {
            if (!menu.stillValid(player)) return;
            menu.setPerkLevel(Perk.Type.values()[message.msgType], message.add, player.getAbilities().instabuild);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
