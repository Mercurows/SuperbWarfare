package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class ArmorPlate extends Item {
    public ArmorPlate() {
        super(new Properties());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (NBTTool.getTag(stack).getBoolean("Infinite")) {
            tooltipComponents.add(Component.translatable("des.superbwarfare.armor_plate.infinite").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        ItemStack armor = playerIn.getItemBySlot(EquipmentSlot.CHEST);

        if (armor == ItemStack.EMPTY) return InteractionResultHolder.fail(stack);

        int armorLevel = 1;
        if (armor.is(ModTags.Items.MILITARY_ARMOR)) {
            armorLevel = 2;
        } else if (armor.is(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
            armorLevel = 3;
        }

        if (NBTTool.getTag(armor).getDouble("ArmorPlate") < armorLevel * 15) {
            playerIn.startUsingItem(handIn);
        }

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        if (!pLevel.isClientSide) {
            ItemStack armor = pLivingEntity.getItemBySlot(EquipmentSlot.CHEST);

            int armorLevel = 1;
            if (armor.is(ModTags.Items.MILITARY_ARMOR)) {
                armorLevel = 2;
            } else if (armor.is(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
                armorLevel = 3;
            }

            var tag = NBTTool.getTag(armor);
            tag.putDouble("ArmorPlate", Mth.clamp(tag.getDouble("ArmorPlate") + 15, 0, armorLevel * 15));
            NBTTool.saveTag(armor, tag);

            if (pLivingEntity instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound((Entity) null, serverPlayer.getOnPos(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.5f, 1);
            }

            if (pLivingEntity instanceof Player player && !player.isCreative() && !NBTTool.getTag(pStack).getBoolean("Infinite")) {
                pStack.shrink(1);
            }
        }

        return super.finishUsingItem(pStack, pLevel, pLivingEntity);
    }

    @Override
    @ParametersAreNonnullByDefault
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 20;
    }

    public static ItemStack getInfiniteInstance() {
        ItemStack stack = new ItemStack(ModItems.ARMOR_PLATE.get());
        final var tag = NBTTool.getTag(stack);
        tag.putBoolean("Infinite", true);
        NBTTool.saveTag(stack, tag);
        return stack;
    }
}
