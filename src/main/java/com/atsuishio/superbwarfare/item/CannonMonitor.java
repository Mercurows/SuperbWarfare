package com.atsuishio.superbwarfare.item;

import net.minecraft.world.item.Item;

public class CannonMonitor extends Item {

    public CannonMonitor() {
        super(new Properties().stacksTo(1));
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
