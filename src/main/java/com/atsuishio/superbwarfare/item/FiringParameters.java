package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.client.screens.FiringParametersScreen;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class FiringParameters extends Item implements ItemScreenProvider {

    public record Parameters(BlockPos pos, int radius, boolean isDepressed) {
        public Parameters(BlockPos pos, boolean isDepressed) {
            this(pos, 0, isDepressed);
        }

        public Parameters(BlockPos pos) {
            this(pos, 0, false);
        }

        public Parameters() {
            this(new BlockPos(0, 0, 0));
        }
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
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        TooltipTool.addScreenProviderText(tooltipComponents);

        var parameters = stack.get(ModDataComponents.FIRING_PARAMETERS);
        var pos = new BlockPos(0, 0, 0);
        var radius = 0;
        var isDepressed = false;
        if (parameters != null) {
            pos = parameters.pos;
            radius = parameters.radius;
            isDepressed = parameters.isDepressed;
        }

        tooltipComponents.add(Component.translatable("tips.superbwarfare.mortar.target_pos").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[" + pos.getX()
                        + ", " + pos.getY()
                        + ", " + pos.getZ() + "]")));
        tooltipComponents.add(Component.translatable("tips.superbwarfare.mortar.target_pos.radius", radius).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                isDepressed
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
