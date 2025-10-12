package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
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

    public String soundName;
    public double x;
    public double y;
    public double z;
    public float radius;

    public SoundClientMessage(String soundName, double x, double y, double z, float radius) {
        this.soundName = soundName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    public static void encode(SoundClientMessage message, FriendlyByteBuf buffer) {
        // TODO 不知有没有方法直接传一个SoundEvent进去
        buffer.writeUtf(message.soundName);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeFloat(message.radius);
    }

    public static SoundClientMessage decode(FriendlyByteBuf buffer) {
        return new SoundClientMessage(buffer.readUtf(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat());
    }

    public static void handler(SoundClientMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> handleSoundClient(message.soundName, message.x, message.y, message.z, message.radius, context)));
        context.get().setPacketHandled(true);
    }

    public static void handleSoundClient(String soundName, double x, double y, double z, float radius, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(soundName));

            if (sound != null && player.level().isClientSide) {

                double distance = player.position().distanceTo(new Vec3(x, y, z));
                Mod.queueClientWork((int) (distance / 17), () -> {
                    player.level().playSound(player, x, y, z, sound, SoundSource.BLOCKS, radius, 1);
                });
            }
        }
    }

    public static void playDistantSound(ServerLevel serverLevel, String soundName, Vec3 pos, float radius) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        List<ServerPlayer> players = serverLevel.getPlayers(p -> p.distanceToSqr(pos) < radius * radius * 256);

        for (var serverPlayer : players) {
            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SoundClientMessage(soundName, x, y, z, radius));
        }
    }
}
