package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class ClientLauncherImageTooltip extends ClientGunImageTooltip {

    public ClientLauncherImageTooltip(GunImageComponent tooltip) {
        super(tooltip);
    }

    @Override
    protected Component getDamageComponent() {
        double damage = data.damage();
        int perkLevel = data.perk.getLevel(ModPerks.MICRO_MISSILE);
        if (perkLevel > 0) damage *= 1.1f + perkLevel * 0.1f;

        double explosionDamage = data.explosionDamage();

        return Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("").withStyle(ChatFormatting.RESET))
                .append(Component.literal(FormatTool.format1D(damage)).withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("").withStyle(ChatFormatting.RESET))
                        .append(Component.literal(" + " + FormatTool.format1D(explosionDamage)).withStyle(ChatFormatting.GOLD)));
    }
}
