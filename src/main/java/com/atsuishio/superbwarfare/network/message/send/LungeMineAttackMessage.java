package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record LungeMineAttackMessage(int msgType, UUID uuid, Vec3 pos) implements CustomPacketPayload {
    public static final Type<LungeMineAttackMessage> TYPE = new Type<>(Mod.loc("lunge_mine_melee_attack"));

    public static final StreamCodec<ByteBuf, LungeMineAttackMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            LungeMineAttackMessage::msgType,
            UUIDUtil.STREAM_CODEC,
            LungeMineAttackMessage::uuid,
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, Vec3::x,
                    ByteBufCodecs.DOUBLE, Vec3::y,
                    ByteBufCodecs.DOUBLE, Vec3::z,
                    Vec3::new
            ),
            LungeMineAttackMessage::pos,
            LungeMineAttackMessage::new
    );

    public static void handler(LungeMineAttackMessage message, final IPayloadContext context) {
        Player player = context.player();
        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModItems.LUNGE_MINE.get())) {
            if (message.msgType == 0) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                Entity lookingEntity = EntityFindUtil.findEntity(player.level(), String.valueOf(message.uuid));
                if (lookingEntity != null) {
                    DamageHandler.doDamage(lookingEntity, ModDamageTypes.causeLungeMineDamage(player.level().registryAccess(), player, player), lookingEntity instanceof VehicleEntity ? 600 : 150);
                    causeLungeMineExplode(player, lookingEntity);
                }
            } else if (message.msgType == 1) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }

                new CustomExplosion.Builder(player)
                        .damage(60)
                        .radius(4)
                        .causeVanillaExplosion()
                        .damageMultiplier(1.25f)
                        .withParticleType(ParticleTool.ParticleType.MEDIUM)
                        .position(message.pos)
                        .explode();
            }
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    public static void causeLungeMineExplode(Entity attacker, Entity target) {
        new CustomExplosion.Builder(target)
                .damage(60)
                .radius(4)
                .attacker(attacker)
                .causeVanillaExplosion()
                .damageMultiplier(1.25f)
                .causeVanillaExplosion()
                .damageMultiplier(1.25F)
                .withParticleType(ParticleTool.ParticleType.MEDIUM)
                .explode();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
