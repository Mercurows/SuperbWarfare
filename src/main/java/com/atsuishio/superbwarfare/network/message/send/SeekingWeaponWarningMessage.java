package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SeekingWeaponWarningMessage(boolean lockOn, UUID uuid) implements CustomPacketPayload {
    public static final Type<SeekingWeaponWarningMessage> TYPE = new Type<>(Mod.loc("seeking_weapon_warning"));

    public static final StreamCodec<ByteBuf, SeekingWeaponWarningMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SeekingWeaponWarningMessage::lockOn,
            UUIDUtil.STREAM_CODEC,
            SeekingWeaponWarningMessage::uuid,
            SeekingWeaponWarningMessage::new
    );


    public static void handler(SeekingWeaponWarningMessage message, final IPayloadContext context) {
        var player = context.player();
        Entity entity = EntityFindUtil.findEntity(player.level(), String.valueOf(message.uuid));

        if (entity != null) {
            entity.level().playSound(null, entity.getOnPos(), entity instanceof Pig ? SoundEvents.PIG_HURT : message.lockOn ? ModSounds.LOCKED_WARNING.get() : ModSounds.LOCKING_WARNING.get(), SoundSource.PLAYERS, 1, 1f);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
