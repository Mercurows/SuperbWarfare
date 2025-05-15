package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.data.gun.GunData;
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
        if (!(stack.getItem() instanceof GunItem)) return;

        var data = GunData.from(stack);
        if (message.isAdd) {
            data.sensitivity.set(Math.min(10, data.sensitivity.get() + 1));
        } else {
            data.sensitivity.set(Math.max(-10, data.sensitivity.get() - 1));
        }
        data.save();
        player.displayClientMessage(Component.translatable("tips.superbwarfare.sensitivity", data.sensitivity.get()), true);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
