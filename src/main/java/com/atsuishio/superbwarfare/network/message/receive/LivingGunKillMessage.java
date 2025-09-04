package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record LivingGunKillMessage(int attackerId, int targetId, boolean headshot, ResourceKey<DamageType> damageType) {

    public static void encode(LivingGunKillMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.attackerId);
        buffer.writeInt(message.targetId);
        buffer.writeBoolean(message.headshot);
        buffer.writeResourceKey(message.damageType);
    }

    public static LivingGunKillMessage decode(FriendlyByteBuf buffer) {
        int attackerId = buffer.readInt();
        int targetId = buffer.readInt();
        boolean headshot = buffer.readBoolean();
        ResourceKey<DamageType> damageType = buffer.readResourceKey(Registries.DAMAGE_TYPE);
        return new LivingGunKillMessage(attackerId, targetId, headshot, damageType);
    }

    public static void handler(LivingGunKillMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
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
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleLivingKillMessage(attacker, target, message.headshot, message.damageType, ctx));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
