package com.atsuishio.superbwarfare.item.common;

import com.atsuishio.superbwarfare.entity.MedicalKitEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class MedicalKitItem extends Item {

    public MedicalKitItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.medical_kit").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand handIn) {
        ItemStack stack = player.getItemInHand(handIn);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                float randomRot = (float) Mth.clamp((2 * Math.random() - 1) * 180, -180, 180);
                MedicalKitEntity entity = new MedicalKitEntity(ModEntities.MEDICAL_KIT.get(), level);
                entity.moveTo(player.getX(), player.getEyeY() - 0.25, player.getZ(), randomRot, 0);
                entity.setYBodyRot(randomRot);
                entity.setYHeadRot(randomRot);
                entity.setDeltaMovement(0.8 * player.getLookAngle().x, 0.8 * player.getLookAngle().y, 0.8 * player.getLookAngle().z);
                level.addFreshEntity(entity);
            }
            player.getCooldowns().addCooldown(this, 25);
            return InteractionResultHolder.success(stack);
        } else if (player.getHealth() < player.getMaxHealth()) {
            player.startUsingItem(handIn);
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack pStack, Level pLevel, @NotNull LivingEntity pLivingEntity) {
        if (!pLevel.isClientSide) {
            treat(pLivingEntity);

            if (pLivingEntity instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound((Entity) null, serverPlayer.getOnPos(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.5f, 1);
            }

            if (pLivingEntity instanceof Player player) {
                player.getCooldowns().addCooldown(pStack.getItem(), 25);
            }

            if (pLivingEntity instanceof Player player && !player.isCreative()) {
                pStack.shrink(1);
            }
        }

        return super.finishUsingItem(pStack, pLevel, pLivingEntity);
    }

    @Override
    @ParametersAreNonnullByDefault
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40;
    }

    public static void treat(LivingEntity living) {
        living.heal(5 + 0.25f * living.getMaxHealth());
        living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false), living);
    }
}
