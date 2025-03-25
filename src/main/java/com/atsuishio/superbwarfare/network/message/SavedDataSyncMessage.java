package com.atsuishio.superbwarfare.network.message;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.capability.player.ModVariables;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SavedDataSyncMessage(
        int messageType,
        SavedData data,
        HolderLookup.Provider registries
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SavedDataSyncMessage> TYPE = new CustomPacketPayload.Type<>(ModUtils.loc("saved_data_sync"));

    public static final StreamCodec<ByteBuf, SavedDataSyncMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SavedDataSyncMessage::messageType,
            ByteBufCodecs.COMPOUND_TAG,
            SavedDataSyncMessage::getNBT,
            (type, nbt) -> new SavedDataSyncMessage(type, null, null)
    );

    public CompoundTag getNBT() {
        return this.data.save(new CompoundTag(), registries);
    }

    public static void handler(final SavedDataSyncMessage message, final IPayloadContext context) {
        if (message.data != null && message.messageType != 0) {
            ModVariables.WorldVariables.clientSide = (ModVariables.WorldVariables) message.data;
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
