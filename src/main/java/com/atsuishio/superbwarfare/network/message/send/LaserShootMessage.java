package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class LaserShootMessage {

    private final double damage;
    private final UUID uuid;
    private final boolean headshot;

    public LaserShootMessage(double damage, UUID uuid, boolean headshot) {
        this.damage = damage;
        this.uuid = uuid;
        this.headshot = headshot;
    }

    public static LaserShootMessage decode(FriendlyByteBuf buffer) {
        return new LaserShootMessage(buffer.readDouble(), buffer.readUUID(), buffer.readBoolean());
    }

    public static void encode(LaserShootMessage message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.damage);
        buffer.writeUUID(message.uuid);
        buffer.writeBoolean(message.headshot);
    }

    public static void handler(LaserShootMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                pressAction(context.getSender(), message.damage, message.uuid, message.headshot);
            }
        });
        context.setPacketHandled(true);
    }

    public static void pressAction(ServerPlayer player, double damage, UUID uuid, boolean headshot) {
        Level level = player.level();

        Entity entity = EntityFindUtil.findEntity(level, String.valueOf(uuid));

        if (entity != null) {
            if (headshot) {
                DamageHandler.doDamage(entity, ModDamageTypes.causeLaserHeadshotDamage(level.registryAccess(), player, player), (float) (2 * damage));
                player.level().playSound(null, player.blockPosition(), ModSounds.HEADSHOT.get(), SoundSource.VOICE, 0.1f, 1);
                Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(1, 5));
            } else {
                DamageHandler.doDamage(entity, ModDamageTypes.causeLaserDamage(level.registryAccess(), player, player), (float) damage);
                player.level().playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 0.1f, 1);
                Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
            }
            entity.invulnerableTime = 0;
        }
    }
}
