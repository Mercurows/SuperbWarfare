package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record DogTagEditorMessage(int containerId, ItemStack stack) implements CustomPacketPayload {

    public static final Type<DogTagEditorMessage> TYPE = new Type<>(Mod.loc("dog_tag_editor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DogTagEditorMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DogTagEditorMessage::containerId,
            ItemStack.STREAM_CODEC, DogTagEditorMessage::stack,
            DogTagEditorMessage::new
    );


    public static void handler(DogTagEditorMessage message) {
        ClientPacketHandler.handleDogTagEditorMessage(message.containerId, message.stack);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
