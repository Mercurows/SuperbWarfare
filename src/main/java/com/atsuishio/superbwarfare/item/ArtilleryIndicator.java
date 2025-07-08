package com.atsuishio.superbwarfare.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class ArtilleryIndicator extends Item {

    public ArtilleryIndicator() {
        super(new Properties().stacksTo(1));
    }

    @Override
    @ParametersAreNonnullByDefault
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 1200;
    }
    
    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        playerIn.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 1.0F);
        return ItemUtils.startUsingInstantly(worldIn, playerIn, handIn);
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        this.stopUsing(pLivingEntity);
        return pStack;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity, int pTimeCharged) {
        this.stopUsing(pLivingEntity);
    }

    private void stopUsing(LivingEntity pUser) {
        pUser.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
    }

//    @Override
//    public @NotNull InteractionResult useOn(UseOnContext pContext) {
//        ItemStack stack = pContext.getItemInHand();
//        BlockPos pos = pContext.getClickedPos();
//        pos = pos.relative(pContext.getClickedFace());
//        Player player = pContext.getPlayer();
//        if (player == null) return InteractionResult.PASS;
//
//        if (player.isShiftKeyDown()) {
//            stack.getOrCreateTag().putDouble("TargetX", pos.getX());
//            stack.getOrCreateTag().putDouble("TargetY", pos.getY());
//            stack.getOrCreateTag().putDouble("TargetZ", pos.getZ());
//        }
//
//        return InteractionResult.SUCCESS;
//    }

//    @Override
//    @ParametersAreNonnullByDefault
//    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
//        if (!player.isCrouching()) return InteractionResultHolder.pass(player.getItemInHand(usedHand));
//
//        var stack = player.getItemInHand(usedHand);
//        var isDepressed = !stack.getOrCreateTag().getBoolean("IsDepressed");
//
//        stack.getOrCreateTag().putBoolean("IsDepressed", isDepressed);
//
//        player.displayClientMessage(Component.translatable(
//                isDepressed
//                        ? "tips.superbwarfare.mortar.target_pos.depressed_trajectory"
//                        : "tips.superbwarfare.mortar.target_pos.lofted_trajectory"
//        ).withStyle(ChatFormatting.GREEN), true);
//
//        return InteractionResultHolder.success(stack);
//    }
//
//    @Override
//    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
//        pTooltipComponents.add(Component.translatable("tips.superbwarfare.mortar.target_pos").withStyle(ChatFormatting.GRAY)
//                .append(Component.literal("[" + pStack.getOrCreateTag().getInt("TargetX")
//                        + "," + pStack.getOrCreateTag().getInt("TargetY")
//                        + "," + pStack.getOrCreateTag().getInt("TargetZ") + "]")));
//
//
//        pTooltipComponents.add(Component.translatable(
//                pStack.getOrCreateTag().getBoolean("IsDepressed")
//                        ? "tips.superbwarfare.mortar.target_pos.depressed_trajectory"
//                        : "tips.superbwarfare.mortar.target_pos.lofted_trajectory"
//        ).withStyle(ChatFormatting.GRAY));
//    }
}
