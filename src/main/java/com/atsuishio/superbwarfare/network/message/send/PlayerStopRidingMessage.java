package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientSetMotionMessage;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class PlayerStopRidingMessage {

    private final boolean ejection;

    public PlayerStopRidingMessage(boolean ejection) {
        this.ejection = ejection;
    }

    public static PlayerStopRidingMessage decode(FriendlyByteBuf buffer) {
        return new PlayerStopRidingMessage(buffer.readBoolean());
    }

    public static void encode(PlayerStopRidingMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.ejection);
    }

    public static void handler(PlayerStopRidingMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (player.getVehicle() instanceof VehicleEntity vehicle) {
                if (message.ejection) {
                    var vec = vehicle.getDismountMovement(player, vehicle.getTagSeatIndex(player));
                    Mod.queueServerWork(1, () -> Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientSetMotionMessage(vec)));
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
                player.addEffect(new MobEffectInstance(ModMobEffects.STRIKE_PROTECTION.get(), 10, 0, false, true), player);
            }
        });
        context.setPacketHandled(true);
    }
}
