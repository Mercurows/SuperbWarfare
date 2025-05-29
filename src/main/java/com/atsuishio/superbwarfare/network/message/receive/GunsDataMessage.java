package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.tools.BufferSerializer;
import com.atsuishio.superbwarfare.tools.GunsTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record GunsDataMessage(List<DefaultGunData> data) implements CustomPacketPayload {
    public static final Type<GunsDataMessage> TYPE = new Type<>(Mod.loc("set_guns_data"));


    public static final StreamCodec<FriendlyByteBuf, GunsDataMessage> STREAM_CODEC = StreamCodec.ofMember(
            (obj, buf) -> {
                buf.writeVarInt(obj.data.size());
                for (var data : obj.data) {
                    buf.writeBytes(BufferSerializer.serialize(data).copy());
                }
            },
            (buf) -> {
                var size = buf.readVarInt();
                var list = new ArrayList<DefaultGunData>();
                for (var i = 0; i < size; i++) {
                    list.add(BufferSerializer.deserialize(buf, new DefaultGunData()));
                }
                return new GunsDataMessage(list);
            }
    );

    public static GunsDataMessage create() {
        return new GunsDataMessage(GunsTool.gunsData.values().stream().toList());
    }

    public static void handler(final GunsDataMessage message, final IPayloadContext context) {
        GunsTool.gunsData.clear();

        for (var entry : message.data) {
            if (GunsTool.gunsData.containsKey(entry.id)) continue;
            GunsTool.gunsData.put(entry.id, entry);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
