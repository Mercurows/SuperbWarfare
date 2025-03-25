package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.tools.AmmoType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SniperAmmoBox extends AmmoSupplierItem {

    public SniperAmmoBox() {
        super(AmmoType.SNIPER, 12, new Properties());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("des.superbwarfare.sniper_ammo_box").withStyle(ChatFormatting.GRAY));
    }
}
