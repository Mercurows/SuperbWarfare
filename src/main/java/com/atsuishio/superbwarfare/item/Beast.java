package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.mixin.BeastEntityKiller;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEnumExtensions;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.PlayerGunKillMessage;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class Beast extends SwordItem {

    public Beast() {
        super(Tiers.NETHERITE, new Properties()
                .stacksTo(1)
                .rarity(ModEnumExtensions.getLegendary())
                .setNoRepair()
                .durability(114514)
        );
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        beastKill(attacker, target);
        return true;
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack stack) {
        return false;
    }

    public static void beastKill(@Nullable Entity attacker, @NotNull Entity target) {
        if (target.level().isClientSide ||
                (target instanceof LivingEntity living && living.isDeadOrDying())
                || target instanceof TargetEntity
        ) return;

        if (target instanceof DPSGeneratorEntity generator) {
            generator.beastCharge();
            return;
        }

        if (attacker instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
            var holder = Holder.direct(ModSounds.INDICATION.get());
            player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.level().random.nextLong()));

            var box = target.getBoundingBox();
            ((ServerLevel) attacker.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + .5, target.getZ(),
                    1000,
                    box.getXsize() / 2.5, box.getYsize() / 3, box.getZsize() / 2.5,
                    0
            );

            if (MiscConfig.SEND_KILL_FEEDBACK.get()) {
                PacketDistributor.sendToAllPlayers(new PlayerGunKillMessage(player.getId(), target.getId(), false, ModDamageTypes.BEAST));
            }
        }

        if (target instanceof ServerPlayer victim) {
            victim.setHealth(0);
            victim.level().players().forEach(
                    p -> p.sendSystemMessage(
                            Component.translatable("death.attack.beast_gun",
                                    victim.getDisplayName(),
                                    attacker != null ? attacker.getDisplayName() : ""
                            )
                    )
            );
        } else {
            if (target instanceof LivingEntity living) {
                BeastEntityKiller.getInstance(living).sbw$kill();
                living.setHealth(0);
            }
            target.level().broadcastEntityEvent(target, (byte) 60);
            target.remove(Entity.RemovalReason.KILLED);
            target.gameEvent(GameEvent.ENTITY_DIE);
        }

        target.level().playSound(target, new BlockPos((int) target.getX(), (int) target.getY(), (int) target.getZ()), ModSounds.OUCH.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull AABB getSweepHitBox(ItemStack stack, Player player, Entity target) {
        return super.getSweepHitBox(stack, player, target).inflate(3);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        var target = TraceTool.findMeleeEntity(entity, 51.4);
        if (target != null) {
            beastKill(entity, target);
        }
        return super.onEntitySwing(stack, entity, hand);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        beastKill(player, entity);
        return super.onLeftClickEntity(stack, player, entity);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.beast").withColor(0xa56855));
    }
}
