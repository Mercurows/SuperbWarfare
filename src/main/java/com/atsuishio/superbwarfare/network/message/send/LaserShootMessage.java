package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


public record LaserShootMessage(
        double damage,
        UUID id,
        boolean headshot
) implements CustomPacketPayload {
    public static final Type<LaserShootMessage> TYPE = new Type<>(Mod.loc("laser_shoot"));

    public static final StreamCodec<ByteBuf, LaserShootMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            LaserShootMessage::damage,
            UUIDUtil.STREAM_CODEC,
            LaserShootMessage::id,
            ByteBufCodecs.BOOL,
            LaserShootMessage::headshot,
            LaserShootMessage::new
    );

    public static void handler(final LaserShootMessage message, final IPayloadContext context) {
        pressAction((ServerPlayer) context.player(), message.damage, message.id, message.headshot);
    }

    public static void pressAction(ServerPlayer player, double damage, UUID uuid, boolean headshot) {
        Level level = player.level();

        Entity entity = EntityFindUtil.findEntity(level, String.valueOf(uuid));

        if (entity != null) {
            if (headshot) {
                entity.hurt(ModDamageTypes.causeLaserHeadshotDamage(level.registryAccess(), player, player), (float) (2 * damage));
                player.level().playSound(null, player.blockPosition(), ModSounds.HEADSHOT.get(), SoundSource.VOICE, 0.1f, 1);
                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(1, 5));
            } else {
                entity.hurt(ModDamageTypes.causeLaserDamage(level.registryAccess(), player, player), (float) damage);
                player.level().playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 0.1f, 1);
                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
            }
            entity.invulnerableTime = 0;
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
