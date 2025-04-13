package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.gun.data.DefaultGunData;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record GunsDataMessage(Map<String, String> gunsData) implements CustomPacketPayload {
    public static final Type<GunsDataMessage> TYPE = new Type<>(Mod.loc("set_guns_data"));


    public static final StreamCodec<ByteBuf, GunsDataMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(
                    HashMap::new,
                    ByteBufCodecs.STRING_UTF8,
                    ByteBufCodecs.STRING_UTF8
            ),
            GunsDataMessage::gunsData,
            GunsDataMessage::new
    );

    private static final Gson GSON = new Gson();

    public static GunsDataMessage create() {
        var map = new HashMap<String, String>();
        for (var entry : GunsTool.gunsData.entrySet()) {
            map.put(entry.getKey(), GSON.toJson(entry.getValue()));
        }
        return new GunsDataMessage(map);
    }

    public static void handler(final GunsDataMessage message, final IPayloadContext context) {
        GunsTool.gunsData.clear();
        for (var entry : message.gunsData.entrySet()) {
            GunsTool.gunsData.put(entry.getKey(), GSON.fromJson(entry.getValue(), DefaultGunData.class));
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
