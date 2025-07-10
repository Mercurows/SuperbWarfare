package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.init.ModItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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

        ItemStack stack = serverPlayer.getMainHandItem();
        if (!stack.is(ModItems.DOG_TAG.get())) return;

        stack.set(ModDataComponents.DOG_TAG_IMAGE, message.colors);

        if (!message.name.isEmpty()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(message.name));
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
