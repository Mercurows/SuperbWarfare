package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.client.KillMessageConfig;
import com.atsuishio.superbwarfare.event.KillMessageHandler;
import com.atsuishio.superbwarfare.tools.LivingKillRecord;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class LivingGunKillMessage implements CustomPacketPayload {
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


    public LivingGunKillMessage(int attackerId, int targetId, boolean headshot, ResourceKey<DamageType> damageType) {
        this.attackerId = attackerId;
        this.targetId = targetId;
        this.headshot = headshot;
        this.location = damageType.location();
    }

    public LivingGunKillMessage(int attackerId, int targetId, boolean headshot, ResourceLocation location) {
        this.attackerId = attackerId;
        this.targetId = targetId;
        this.headshot = headshot;
        this.location = location;
    }

    public static final Type<LivingGunKillMessage> TYPE = new Type<>(Mod.loc("player_gun_kill"));

    public static final StreamCodec<ByteBuf, LivingGunKillMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            LivingGunKillMessage::getAttackerId,
            ByteBufCodecs.INT,
            LivingGunKillMessage::getTargetId,
            ByteBufCodecs.BOOL,
            LivingGunKillMessage::isHeadshot,
            ResourceLocation.STREAM_CODEC,
            LivingGunKillMessage::getLocation,
            LivingGunKillMessage::new
    );


    public static void handler(LivingGunKillMessage message, final IPayloadContext context) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            var entity = level.getEntity(message.attackerId);
            LivingEntity attacker;
            if (entity instanceof LivingEntity living) {
                if (living instanceof Player player) {
                    attacker = player;
                } else if (living instanceof OwnableEntity ownableEntity && ownableEntity.getOwner() instanceof Player) {
                    attacker = living;
                } else {
                    attacker = null;
                }
            } else {
                attacker = null;
            }
            Entity target = level.getEntity(message.targetId);

            if (attacker != null && target != null) {
                var type = message.getDamageType();

                if (KillMessageHandler.QUEUE.size() >= KillMessageConfig.KILL_MESSAGE_COUNT.get()) {
                    KillMessageHandler.QUEUE.poll();
                }
                KillMessageHandler.QUEUE.offer(new LivingKillRecord(attacker, target, attacker.getMainHandItem(), message.headshot, type));
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
