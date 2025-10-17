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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

public record SeekingWeaponShootMessage(
        double spread,
        boolean zoom,
        @Nullable UUID uuid,
        Vector3f targetPos
) implements CustomPacketPayload {
    public static final Type<SeekingWeaponShootMessage> TYPE = new Type<>(Mod.loc("seeking_weapon_shoot"));

    public static final StreamCodec<ByteBuf, SeekingWeaponShootMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            SeekingWeaponShootMessage::spread,
            ByteBufCodecs.BOOL,
            SeekingWeaponShootMessage::zoom,
            UUIDUtil.STREAM_CODEC,
            SeekingWeaponShootMessage::uuid,
            ByteBufCodecs.VECTOR3F,
            SeekingWeaponShootMessage::targetPos,
            SeekingWeaponShootMessage::new
    );

    public static void handler(SeekingWeaponShootMessage message, final IPayloadContext context) {
        var player = context.player();
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        GunData.from(stack).shoot(player, message.spread, message.zoom, message.uuid, new Vec3(message.targetPos));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
