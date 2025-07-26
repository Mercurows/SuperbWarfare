package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientSetMotionMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public record PlayerStopRidingMessage(boolean ejection) implements CustomPacketPayload {

    public static final Type<PlayerStopRidingMessage> TYPE = new Type<>(Mod.loc("player_stop_riding"));

    public static final StreamCodec<ByteBuf, PlayerStopRidingMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PlayerStopRidingMessage::ejection,
            PlayerStopRidingMessage::new
    );

    public static void handler(PlayerStopRidingMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        var entity = player.getVehicle();
        if (entity instanceof VehicleEntity vehicle) {
            if (message.ejection) {
                var vec = vehicle.getDismountMovement(player, vehicle.getTagSeatIndex(player));
                Mod.queueServerWork(1, () -> PacketDistributor.sendToPlayer(player, new ClientSetMotionMessage(vec.toVector3f())));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MEDIUM_ROCKET_FIRE.get(), SoundSource.PLAYERS, 4f, 1);
                if (player.level() instanceof ServerLevel serverLevel) {
                    for (int p = 0; p < 8; p++) {
                        Vec3 pPos = player.position().add(vec.scale(p * 0.5));
                        sendParticle(serverLevel, ParticleTypes.CLOUD, pPos.x, pPos.y, pPos.z, 10, 0.5, 0.5, 0.5, 0.05, true);
                        sendParticle(serverLevel, ParticleTypes.FLAME, pPos.x, pPos.y, pPos.z, 20, 0.5, 0.5, 0.5, 0.05, true);
                        sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, pPos.x, pPos.y, pPos.z, 15, 0.5, 0.5, 0.5, 0.05, true);
                    }
                }
            }
            player.stopRiding();
            player.setJumping(false);
            player.addEffect(new MobEffectInstance(ModMobEffects.STRIKE_PROTECTION, 10, 0, false, true), player);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
