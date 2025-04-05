package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SensitivityMessage(boolean isAdd) implements CustomPacketPayload {
    public static final Type<SensitivityMessage> TYPE = new Type<>(Mod.loc("sensitivity"));

    public static final StreamCodec<ByteBuf, SensitivityMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SensitivityMessage::isAdd,
            SensitivityMessage::new
    );

    public static void handler(SensitivityMessage message, final IPayloadContext context) {
        var player = context.player();

        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;

        var data = GunData.from(stack);
        final var tag = data.getTag();
        if (message.isAdd) {
            tag.putInt("sensitivity", Math.min(10, tag.getInt("sensitivity") + 1));
        } else {
            tag.putInt("sensitivity", Math.max(-10, tag.getInt("sensitivity") - 1));
        }
        data.save();
        player.displayClientMessage(Component.translatable("tips.superbwarfare.sensitivity", tag.getInt("sensitivity")), true);

    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
