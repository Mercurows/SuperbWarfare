package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.client.KillMessageConfig;
import com.atsuishio.superbwarfare.event.KillMessageHandler;
import com.atsuishio.superbwarfare.tools.PlayerKillRecord;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class PlayerGunKillMessage implements CustomPacketPayload {
    private final int attackerId;
    private final int targetId;

    private final boolean headshot;
    private final ResourceLocation location;


    public int getAttackerId() {
        return attackerId;
    }

    public boolean isHeadshot() {
        return headshot;
    }

    public int getTargetId() {
        return targetId;
    }

    public ResourceKey<DamageType> getDamageType() {
        return ResourceKey.create(Registries.DAMAGE_TYPE, this.location);
    }

    public ResourceLocation getLocation() {
        return location;
    }


    public PlayerGunKillMessage(int attackerId, int targetId, boolean headshot, ResourceKey<DamageType> damageType) {
        this.attackerId = attackerId;
        this.targetId = targetId;
        this.headshot = headshot;
        this.location = damageType.location();
    }

    public PlayerGunKillMessage(int attackerId, int targetId, boolean headshot, ResourceLocation location) {
        this.attackerId = attackerId;
        this.targetId = targetId;
        this.headshot = headshot;
        this.location = location;
    }

    public static final Type<PlayerGunKillMessage> TYPE = new Type<>(Mod.loc("player_gun_kill"));

    public static final StreamCodec<ByteBuf, PlayerGunKillMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PlayerGunKillMessage::getAttackerId,
            ByteBufCodecs.INT,
            PlayerGunKillMessage::getTargetId,
            ByteBufCodecs.BOOL,
            PlayerGunKillMessage::isHeadshot,
            ResourceLocation.STREAM_CODEC,
            PlayerGunKillMessage::getLocation,
            PlayerGunKillMessage::new
    );


    public static void handler(PlayerGunKillMessage message, final IPayloadContext context) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Player player = level.getEntity(message.attackerId) instanceof Player ? (Player) level.getEntity(message.attackerId) : null;
            Entity target = level.getEntity(message.targetId);

            if (player != null && target != null) {
                var type = message.getDamageType();

                if (KillMessageHandler.QUEUE.size() >= KillMessageConfig.KILL_MESSAGE_COUNT.get()) {
                    KillMessageHandler.QUEUE.poll();
                }
                KillMessageHandler.QUEUE.offer(new PlayerKillRecord(player, target, player.getMainHandItem(), message.headshot, type));
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
