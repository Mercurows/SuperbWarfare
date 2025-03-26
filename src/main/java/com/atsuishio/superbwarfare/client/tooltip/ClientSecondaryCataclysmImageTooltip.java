package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import net.minecraft.network.chat.Component;

public class ClientSecondaryCataclysmImageTooltip extends ClientEnergyImageTooltip {

    public ClientSecondaryCataclysmImageTooltip(GunImageComponent tooltip) {
        super(tooltip);
    }

    @Override
    protected Component getDamageComponent() {
        // TODO GunInfo
//        double damage = GunsTool.getGunDoubleTag(stack, "Damage", 0) * TooltipTool.perkDamage(stack);
//        int perkLevel = PerkHelper.getItemPerkLevel(ModPerks.MICRO_MISSILE.get(), stack);
//        if (perkLevel > 0) damage *= 1.1f + perkLevel * 0.1f;
//
//        double explosionDamage = GunsTool.getGunDoubleTag(stack, "ExplosionDamage", 0);
//
//        return Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
//                .append(Component.literal("").withStyle(ChatFormatting.RESET))
//                .append(Component.literal(FormatTool.format1D(damage)).withStyle(ChatFormatting.GREEN)
//                        .append(Component.literal("").withStyle(ChatFormatting.RESET))
//                        .append(Component.literal(" + " + FormatTool.format1D(explosionDamage)).withStyle(ChatFormatting.GOLD)));
        return Component.literal("");
    }
}
