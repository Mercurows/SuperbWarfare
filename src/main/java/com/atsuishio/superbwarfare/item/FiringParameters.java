package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.client.screens.FiringParametersScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class FiringParameters extends Item implements ItemScreenProvider {

    public FiringParameters() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext pContext) {
        ItemStack stack = pContext.getItemInHand();
        BlockPos pos = pContext.getClickedPos();
        pos = pos.relative(pContext.getClickedFace());
        Player player = pContext.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            stack.getOrCreateTag().putDouble("TargetX", pos.getX());
            stack.getOrCreateTag().putDouble("TargetY", pos.getY());
            stack.getOrCreateTag().putDouble("TargetZ", pos.getZ());
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!player.isCrouching()) return InteractionResultHolder.pass(player.getItemInHand(usedHand));

        var stack = player.getItemInHand(usedHand);
        var isDepressed = !stack.getOrCreateTag().getBoolean("IsDepressed");

        stack.getOrCreateTag().putBoolean("IsDepressed", isDepressed);

        player.displayClientMessage(Component.translatable(
                isDepressed
                        ? "tips.superbwarfare.mortar.target_pos.depressed_trajectory"
                        : "tips.superbwarfare.mortar.target_pos.lofted_trajectory"
        ).withStyle(ChatFormatting.GREEN), true);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("tips.superbwarfare.mortar.target_pos").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[" + pStack.getOrCreateTag().getInt("TargetX")
                        + "," + pStack.getOrCreateTag().getInt("TargetY")
                        + "," + pStack.getOrCreateTag().getInt("TargetZ") + "]")));


        pTooltipComponents.add(Component.translatable(
                pStack.getOrCreateTag().getBoolean("IsDepressed")
                        ? "tips.superbwarfare.mortar.target_pos.depressed_trajectory"
                        : "tips.superbwarfare.mortar.target_pos.lofted_trajectory"
        ).withStyle(ChatFormatting.GRAY));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Screen getItemScreen(ItemStack stack, Player player, InteractionHand hand) {
        return new FiringParametersScreen(stack, hand);
    }
}
