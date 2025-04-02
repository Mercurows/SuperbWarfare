package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.component.ModDataComponents;
import net.minecraft.ChatFormatting;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class FiringParameters extends Item {

    public record Parameters(BlockPos pos, boolean isDepressed) {
    }

    public FiringParameters() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext pContext) {
        Player player = pContext.getPlayer();
        if (player == null) return InteractionResult.PASS;

        ItemStack stack = pContext.getItemInHand();
        BlockPos pos = pContext.getClickedPos();
        pos = pos.relative(pContext.getClickedFace());

        var parameters = stack.get(ModDataComponents.FIRING_PARAMETERS);
        var isDepressed = parameters != null && parameters.isDepressed();

        stack.set(ModDataComponents.FIRING_PARAMETERS, new Parameters(pos, isDepressed));
        return InteractionResult.SUCCESS;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!player.isCrouching()) return InteractionResultHolder.pass(player.getItemInHand(usedHand));

        var stack = player.getItemInHand(usedHand);
        var parameters = stack.get(ModDataComponents.FIRING_PARAMETERS);
        if (parameters == null) return InteractionResultHolder.fail(stack);

        var isDepressed = !parameters.isDepressed();
        stack.set(ModDataComponents.FIRING_PARAMETERS, new Parameters(parameters.pos(), isDepressed));
        player.displayClientMessage(Component.translatable(
                isDepressed
                        ? "tips.superbwarfare.mortar.target_pos.depressed_trajectory"
                        : "tips.superbwarfare.mortar.target_pos.lofted_trajectory"
        ).withStyle(ChatFormatting.GREEN), true);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        var parameters = stack.get(ModDataComponents.FIRING_PARAMETERS);
        if (parameters == null) return;

        var pos = parameters.pos();
        tooltipComponents.add(Component.translatable("tips.superbwarfare.mortar.target_pos")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("["
                        + pos.getX() + ","
                        + pos.getY() + ","
                        + pos.getZ() + "]")
                )
        );

        tooltipComponents.add(Component.translatable(
                parameters.isDepressed
                        ? "tips.superbwarfare.mortar.target_pos.depressed_trajectory"
                        : "tips.superbwarfare.mortar.target_pos.lofted_trajectory"
        ).withStyle(ChatFormatting.GRAY));
    }
}
