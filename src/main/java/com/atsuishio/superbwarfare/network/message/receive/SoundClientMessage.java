package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
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

public record SoundClientMessage(
        ResourceLocation location,
        double x,
        double y,
        double z,
        float radius,
        float pitch
) implements CustomPacketPayload {

    public static final Type<SoundClientMessage> TYPE = new Type<>(Mod.loc("sound_client"));

    public static final StreamCodec<ByteBuf, SoundClientMessage> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            SoundClientMessage::location,
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
        handleSoundClient(message);
    }

    public static void handleSoundClient(SoundClientMessage message) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(message.location());
        if (sound == null) return;

        double distance = player.position().distanceTo(new Vec3(message.x(), message.y(), message.z()));
        Mod.queueClientWork((int) (distance / 17),
                () -> player.level().playSound(player, message.x(), message.y(), message.z(), sound, SoundSource.BLOCKS, message.radius(), message.pitch()));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
