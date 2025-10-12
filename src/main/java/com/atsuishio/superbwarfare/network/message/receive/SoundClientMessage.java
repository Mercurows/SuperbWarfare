package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.UUID;

public record SoundClientMessage(
        ResourceLocation location,
        Vector3f pos,
        float radius,
        float pitch,
        UUID uuid
) implements CustomPacketPayload {

    public static final Type<SoundClientMessage> TYPE = new Type<>(Mod.loc("sound_client"));

    public static final StreamCodec<ByteBuf, SoundClientMessage> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            SoundClientMessage::location,
            ByteBufCodecs.VECTOR3F,
            SoundClientMessage::pos,
            ByteBufCodecs.FLOAT,
            SoundClientMessage::radius,
            ByteBufCodecs.FLOAT,
            SoundClientMessage::pitch,
            UUIDUtil.STREAM_CODEC,
            SoundClientMessage::uuid,
            SoundClientMessage::new
    );

    public static void handler(SoundClientMessage message) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.getUUID().equals(message.uuid())) return;

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(message.location());
        if (sound == null) return;

        double distance = player.position().distanceTo(new Vec3(message.pos));
        int time = (int) (distance / 17);

        if (time == 0) {
            player.level().playSound(player, message.pos.x(), message.pos.y(), message.pos.z(), sound, SoundSource.BLOCKS, message.radius(), message.pitch());
        } else {
            Mod.queueClientWork(time,
                    () -> player.level().playSound(player, message.pos.x(), message.pos.y(), message.pos.z(), sound, SoundSource.BLOCKS, message.radius(), message.pitch()));
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
