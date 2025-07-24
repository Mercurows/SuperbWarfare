package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class LungeMineAttackMessage {

    private final int type;
    private final UUID uuid;
    private final BlockHitResult hitResult;

    public LungeMineAttackMessage(int type, UUID uuid, BlockHitResult hitResult) {
        this.type = type;
        this.uuid = uuid;
        this.hitResult = hitResult;
    }

    public static LungeMineAttackMessage decode(FriendlyByteBuf buffer) {
        return new LungeMineAttackMessage(buffer.readInt(), buffer.readUUID(), buffer.readBlockHitResult());
    }

    public static void encode(LungeMineAttackMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
        buffer.writeUUID(message.uuid);
        buffer.writeBlockHitResult(message.hitResult);
    }

    public static void handler(LungeMineAttackMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                Player player = context.getSender();

                ItemStack stack = player.getMainHandItem();

                if (stack.is(ModItems.LUNGE_MINE.get())) {
                    if (message.type == 0) {
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        Entity lookingEntity = EntityFindUtil.findEntity(player.level(), String.valueOf(message.uuid));
                        if (lookingEntity != null) {
                            DamageHandler.doDamage(lookingEntity, ModDamageTypes.causeLungeMineDamage(player.level().registryAccess(), player, player), lookingEntity instanceof VehicleEntity ? 600 : 150);
                            causeLungeMineExplode(player.level(), player, lookingEntity);
                        }
                    } else if (message.type == 1) {
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        CustomExplosion explosion = new CustomExplosion(player.level(), null,
                                ModDamageTypes.causeProjectileBoomDamage(player.level().registryAccess(), player, player), 60,
                                message.hitResult.getLocation().x, message.hitResult.getLocation().y, message.hitResult.getLocation().z, 4f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1.25f);
                        explosion.explode();
                        net.minecraftforge.event.ForgeEventFactory.onExplosionStart(player.level(), explosion);
                        explosion.finalizeExplosion(false);
                        ParticleTool.spawnMediumExplosionParticles(player.level(), message.hitResult.getLocation());

                    }
                    player.swing(InteractionHand.MAIN_HAND);
                }
            }
        });
        context.setPacketHandled(true);
    }

    public static void causeLungeMineExplode(Level pLevel, Entity entity, Entity pLivingEntity) {
        CustomExplosion explosion = new CustomExplosion(pLevel, pLivingEntity,
                ModDamageTypes.causeProjectileBoomDamage(pLevel.registryAccess(), pLivingEntity, entity), 60,
                pLivingEntity.getX(), pLivingEntity.getEyeY(), pLivingEntity.getZ(), 4f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1.25f);
        explosion.explode();
        net.minecraftforge.event.ForgeEventFactory.onExplosionStart(pLevel, explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnMediumExplosionParticles(pLevel, pLivingEntity.position());
    }
}
