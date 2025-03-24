package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.component.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FiringParameters extends Item {

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

        stack.set(ModDataComponents.BLOCK_POS, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        var pos = stack.get(ModDataComponents.BLOCK_POS);
        if (pos == null) return;

        tooltipComponents.add(Component.translatable("tips.superbwarfare.mortar.target_pos")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("["
                        + pos.getX() + ","
                        + pos.getY() + ","
                        + pos.getZ() + "]")
                )
        );
    }
}
