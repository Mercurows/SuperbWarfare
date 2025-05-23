package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.menu.DogTagEditorMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record DogTagFinishEditMessage(List<Short> colors, String name) implements CustomPacketPayload {
    public static final Type<DogTagFinishEditMessage> TYPE = new Type<>(Mod.loc("dog_tag_finish_edit"));

    public static final StreamCodec<ByteBuf, DogTagFinishEditMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.SHORT.apply(ByteBufCodecs.list()), DogTagFinishEditMessage::colors,
            ByteBufCodecs.STRING_UTF8, DogTagFinishEditMessage::name,
            DogTagFinishEditMessage::new
    );


    public static void handler(DogTagFinishEditMessage message, final IPayloadContext context) {
        ServerPlayer serverPlayer = (ServerPlayer) context.player();

        if (serverPlayer.containerMenu instanceof DogTagEditorMenu menu) {
            menu.finishEdit(message.colors, message.name);
        }

        serverPlayer.closeContainer();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
