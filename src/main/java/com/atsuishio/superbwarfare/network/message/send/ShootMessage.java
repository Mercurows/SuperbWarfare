package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.data.gun.GunData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ShootMessage(double spread, boolean zoom) implements CustomPacketPayload {

    public static final Type<ShootMessage> TYPE = new Type<>(Mod.loc("shoot"));

    public static final StreamCodec<ByteBuf, ShootMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ShootMessage::spread,
            ByteBufCodecs.BOOL,
            ShootMessage::zoom,
            ShootMessage::new
    );

    public static void handler(final ShootMessage message, final IPayloadContext context) {
        pressAction(context.player(), message.spread, message.zoom);
    }

    public static void pressAction(Player player, double spread, boolean zoom) {
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        data.item.onShoot(data, player, spread, zoom);

        data.save();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
