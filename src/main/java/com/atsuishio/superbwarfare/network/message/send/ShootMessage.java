package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

public record ShootMessage(
        double spread,
        boolean zoom,
        Optional<UUID> uuid,
        Optional<Vector3f> targetPos
) implements CustomPacketPayload {

    public static final Type<ShootMessage> TYPE = new Type<>(Mod.loc("shoot"));

    public static final StreamCodec<ByteBuf, ShootMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ShootMessage::spread,
            ByteBufCodecs.BOOL,
            ShootMessage::zoom,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            ShootMessage::uuid,
            ByteBufCodecs.optional(ByteBufCodecs.VECTOR3F),
            ShootMessage::targetPos,
            ShootMessage::new
    );

    public static void handler(final ShootMessage message, final IPayloadContext context) {
        var player = context.player();
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        if (message.targetPos.isEmpty()) {
            GunData.from(stack).shoot(player, message.spread, message.zoom, message.uuid.orElse(null));
        } else {
            GunData.from(stack).shoot(player, message.spread, message.zoom, message.uuid.orElse(null), new Vec3(message.targetPos.get()));
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
