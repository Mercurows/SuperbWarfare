package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.world.TDMSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record TDMSyncMessage(TDMSavedData data) implements CustomPacketPayload {
    public static final Type<TDMSyncMessage> TYPE = new Type<>(Mod.loc("tdm_sync"));

    public static final StreamCodec<FriendlyByteBuf, TDMSyncMessage> STREAM_CODEC = StreamCodec.ofMember(
            (obj, buf) -> buf.writeCollection(obj.data().getEntities(), FriendlyByteBuf::writeUtf),
            (buf) -> new TDMSyncMessage(new TDMSavedData(buf.readList(FriendlyByteBuf::readUtf)))
    );

    public static void handler(TDMSyncMessage message) {
        ClientEventHandler.tdmSavedData = message.data();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
