package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ShakeClientMessage(
        double time, double radius, double amplitude,
        double x, double y, double z
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShakeClientMessage> TYPE = new CustomPacketPayload.Type<>(Mod.loc("shake_client"));

    public static final StreamCodec<ByteBuf, ShakeClientMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::time,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::radius,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::amplitude,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::x,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::y,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::z,
            ShakeClientMessage::new
    );

    public static void handler(final ShakeClientMessage message, final IPayloadContext context) {
        ClientEventHandler.handleShakeClient(message.time, message.radius, message.amplitude, message.x, message.y, message.z);
    }


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendToNearbyPlayers(Level level, double x, double y, double z, double sendRadius, double time, double radius, double amplitude) {
        var center = new Vec3(x, y, z);

        for (var serverPlayer : level.getEntitiesOfClass(ServerPlayer.class, new AABB(center, center).inflate(sendRadius), e -> true)) {
            PacketDistributor.sendToPlayer(serverPlayer, new ShakeClientMessage(time, radius, amplitude, x, y, z));
        }
    }

    public static void sendToNearbyPlayers(Entity source, double sendRadius, double time, double radius, double amplitude) {
        sendToNearbyPlayers(source.level(), source.getX(), source.getY(), source.getZ(), sendRadius, time, radius, amplitude);
    }
}
