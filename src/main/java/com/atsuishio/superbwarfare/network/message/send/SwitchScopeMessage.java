package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.data.gun.GunData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SwitchScopeMessage(double scroll) implements CustomPacketPayload {
    public static final Type<SwitchScopeMessage> TYPE = new Type<>(Mod.loc("switch_scope"));

    public static final StreamCodec<ByteBuf, SwitchScopeMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            SwitchScopeMessage::scroll,
            SwitchScopeMessage::new
    );

    public static void handler(SwitchScopeMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        var data = GunData.from(stack);
        final var tag = data.tag();
        tag.putBoolean("ScopeAlt", !tag.getBoolean("ScopeAlt"));
        data.save();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
