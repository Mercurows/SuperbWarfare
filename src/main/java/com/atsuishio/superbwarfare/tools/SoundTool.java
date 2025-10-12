package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.network.message.receive.SoundClientMessage;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class SoundTool {

    public static void playLocalSound(Player player, SoundEvent sound) {
        playLocalSound(player, sound, 1.0F, 1.0F);
    }

    public static void playLocalSound(Player player, SoundEvent sound, float volume, float pitch) {
        if (player instanceof ServerPlayer serverPlayer) {
            playLocalSound(serverPlayer, sound, volume, pitch);
        }
    }

    public static void playLocalSound(ServerPlayer player, SoundEvent sound) {
        playLocalSound(player, sound, 1.0F, 1.0F);
    }

    public static void playLocalSound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        playLocalSound(player, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void playLocalSound(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(new Holder.Direct<>(sound),
                source, player.getX(), player.getY(), player.getZ(), volume, pitch, player.level().random.nextLong()));
    }

    public static void stopSound(ServerPlayer player, ResourceLocation sound) {
        stopSound(player, sound, SoundSource.PLAYERS);
    }

    public static void stopSound(ServerPlayer player, ResourceLocation sound, SoundSource source) {
        player.connection.send(new ClientboundStopSoundPacket(sound, source));
    }

    public static void playDistantSound(ServerLevel serverLevel, SoundEvent soundEvent, Vec3 pos, float radius, float pitch, Entity sender) {
        for (var serverPlayer : serverLevel.getPlayers(p -> p.distanceToSqr(pos) < radius * radius * 256)) {
            PacketDistributor.sendToPlayer(serverPlayer, new SoundClientMessage(soundEvent.getLocation(), pos.toVector3f(), radius, pitch, sender == null ? UUID.randomUUID() : sender.getUUID()));
        }
    }
}
