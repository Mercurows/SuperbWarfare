package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.mixin.BeastEntityKiller;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.LivingGunKillMessage;
import com.atsuishio.superbwarfare.tools.RarityTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class Beast extends SwordItem {

    public Beast() {
        super(Tiers.NETHERITE, 0, 0, new Properties()
                .stacksTo(1)
                .rarity(RarityTool.LEGENDARY)
                .setNoRepair()
                .durability(114514)
        );
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        beastKill(attacker, target);
        return true;
    }

    public static void beastKill(@Nullable Entity attacker, @NotNull Entity target) {
        if (target.level().isClientSide) return;

        if (target instanceof TargetEntity) {
            target.hurt(ModDamageTypes.causeBeastDamage(target.level().registryAccess(), attacker, attacker), 114514.1919810F);
            return;
        }

        if (target instanceof DPSGeneratorEntity generator) {
            generator.hurt(ModDamageTypes.causeBeastDamage(generator.level().registryAccess(), attacker, attacker), 114514.1919810F);
            generator.beastCharge();
            return;
        }

        if (attacker instanceof ServerPlayer player) {
            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(1, 5));
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
                Mod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new LivingGunKillMessage(player.getId(), target.getId(), false, ModDamageTypes.BEAST));
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

            target.removalReason = Entity.RemovalReason.KILLED;
            target.getPassengers().forEach(Entity::stopRiding);
            target.stopRiding();

            target.levelCallback.onRemove(Entity.RemovalReason.KILLED);
            target.invalidateCaps();

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
    public boolean canBeHurtBy(@NotNull DamageSource source) {
        return false;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        var target = TraceTool.findMeleeEntity(entity, 51.4);
        if (target != null) {
            beastKill(entity, target);
        }
        return super.onEntitySwing(stack, entity);
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
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.beast").withStyle(Style.EMPTY.withColor(0xa56855)));
    }
}
