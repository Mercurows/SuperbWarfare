package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class Transcript extends Item {

    public Transcript() {
        super(new Properties().stacksTo(1));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.transcript").withStyle(ChatFormatting.GRAY));
        addScoresText(stack, tooltipComponents);
    }

    public void addScoresText(ItemStack stack, List<Component> tooltip) {
        var scores = stack.get(ModDataComponents.TRANSCRIPT_SCORE);
        if (scores == null) scores = List.of();

        int total = 0;
        for (var info : scores) {
            int score = info.getFirst();
            total += score;
            tooltip.add(Component.translatable("des.superbwarfare.transcript.score").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(score + " ").withStyle(score == 10 ? ChatFormatting.GOLD : ChatFormatting.WHITE))
                    .append(Component.translatable("des.superbwarfare.transcript.distance").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(FormatTool.format1D(info.getSecond(), "m")).withStyle(ChatFormatting.WHITE)));
        }

        tooltip.add(Component.translatable("des.superbwarfare.transcript.total").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(total + " ").withStyle(total == 100 ? ChatFormatting.GOLD : ChatFormatting.WHITE)));
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (pPlayer.isCrouching()) {
            ItemStack stack = pPlayer.getItemInHand(pUsedHand);
            stack.set(ModDataComponents.TRANSCRIPT_SCORE, List.of());
            return InteractionResultHolder.success(stack);
        }
        return super.use(pLevel, pPlayer, pUsedHand);
    }
}
