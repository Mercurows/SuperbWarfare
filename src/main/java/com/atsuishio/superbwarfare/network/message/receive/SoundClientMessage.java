package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record SoundClientMessage(
        ResourceLocation soundName,
        double x,
        double y,
        double z,
        float radius,
        float pitch
) implements CustomPacketPayload {

    public static final Type<SoundClientMessage> TYPE = new Type<>(Mod.loc("sound_client"));

    public static final StreamCodec<ByteBuf, SoundClientMessage> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            SoundClientMessage::soundName,
            ByteBufCodecs.DOUBLE,
            SoundClientMessage::x,
            ByteBufCodecs.DOUBLE,
            SoundClientMessage::y,
            ByteBufCodecs.DOUBLE,
            SoundClientMessage::z,
            ByteBufCodecs.FLOAT,
            SoundClientMessage::radius,
            ByteBufCodecs.FLOAT,
            SoundClientMessage::pitch,
            SoundClientMessage::new
    );

    public static void handler(SoundClientMessage message) {
        handleSoundClient(message.soundName, message.x, message.y, message.z, message.radius, message.pitch);
    }

    public static void handleSoundClient(ResourceLocation soundName, double x, double y, double z, float radius, float pitch) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundName);

        if (sound != null && player.level().isClientSide) {

            double distance = player.position().distanceTo(new Vec3(x, y, z));
            Mod.queueClientWork((int) (distance / 17), () -> player.level().playSound(player, x, y, z, sound, SoundSource.BLOCKS, radius, pitch));
        }
    }

    public static void playDistantSound(ServerLevel serverLevel, SoundEvent soundEvent, Vec3 pos, float radius, float pitch) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        List<ServerPlayer> players = serverLevel.getPlayers(p -> p.distanceToSqr(pos) < radius * radius * 256);

        for (var serverPlayer : players) {
            PacketDistributor.sendToPlayer(serverPlayer, new SoundClientMessage(soundEvent.getLocation(), x, y, z, radius, pitch));
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
