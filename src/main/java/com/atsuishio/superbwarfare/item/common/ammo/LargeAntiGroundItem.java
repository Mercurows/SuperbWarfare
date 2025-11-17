package com.atsuishio.superbwarfare.item.common.ammo;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class LargeAntiGroundItem extends Item {

    public LargeAntiGroundItem() {
        super(new Properties().stacksTo(2));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.large_anti_ground_missile").withStyle(ChatFormatting.GRAY));
    }
}