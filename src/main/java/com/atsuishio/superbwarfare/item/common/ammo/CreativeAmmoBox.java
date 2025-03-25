package com.atsuishio.superbwarfare.item.common.ammo;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class CreativeAmmoBox extends Item {

    public CreativeAmmoBox() {
        super(new Properties().rarity(Rarity.EPIC).stacksTo(1));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.creative_ammo_box").withStyle(ChatFormatting.GRAY));
    }
}
