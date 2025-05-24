package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record MeleeAttackMessage(UUID uuid) implements CustomPacketPayload {
    public static final Type<MeleeAttackMessage> TYPE = new Type<>(Mod.loc("melee_attack"));

    public static final StreamCodec<ByteBuf, MeleeAttackMessage> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            MeleeAttackMessage::uuid,
            MeleeAttackMessage::new
    );

    public static void handler(MeleeAttackMessage message, final IPayloadContext context) {
        Player player = context.player();

        Entity lookingEntity = EntityFindUtil.findEntity(player.level(), String.valueOf(message.uuid));
        if (lookingEntity != null) {
            player.level().playSound(null, lookingEntity.getOnPos(), ModSounds.MELEE_HIT.get(), SoundSource.PLAYERS, 1, (float) ((2 * org.joml.Math.random() - 1) * 0.1f + 1.0f));
            player.attack(lookingEntity);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
