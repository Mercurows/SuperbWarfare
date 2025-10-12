package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

public class SoundClientMessage {

    public ResourceLocation soundName;
    public double x;
    public double y;
    public double z;
    public float radius;
    public float pitch;

    public SoundClientMessage(ResourceLocation soundName, double x, double y, double z, float radius, float pitch) {
        this.soundName = soundName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.pitch = pitch;
    }

    public static void encode(SoundClientMessage message, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(message.soundName);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeFloat(message.radius);
        buffer.writeFloat(message.pitch);
    }

    public static SoundClientMessage decode(FriendlyByteBuf buffer) {
        return new SoundClientMessage(buffer.readResourceLocation(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handler(SoundClientMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> handleSoundClient(message.soundName, message.x, message.y, message.z, message.radius, message.pitch, context)));
        context.get().setPacketHandled(true);
    }

    public static void handleSoundClient(ResourceLocation soundName, double x, double y, double z, float radius, float pitch, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundName);

            if (sound != null && player.level().isClientSide) {

                double distance = player.position().distanceTo(new Vec3(x, y, z));
                Mod.queueClientWork((int) (distance / 17), () -> {
                    player.level().playSound(player, x, y, z, sound, SoundSource.BLOCKS, radius, pitch);
                });
            }
        }
    }

    public static void playDistantSound(ServerLevel serverLevel, SoundEvent soundEvent, Vec3 pos, float radius, float pitch) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        List<ServerPlayer> players = serverLevel.getPlayers(p -> p.distanceToSqr(pos) < radius * radius * 256);

        for (var serverPlayer : players) {
            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SoundClientMessage(soundEvent.getLocation(), x, y, z, radius, pitch));
        }
    }
}
