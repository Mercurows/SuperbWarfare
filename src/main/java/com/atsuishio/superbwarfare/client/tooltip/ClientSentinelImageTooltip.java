package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.capabilities.Capabilities;

public class ClientSentinelImageTooltip extends ClientEnergyImageTooltip {

    public ClientSentinelImageTooltip(GunImageComponent tooltip) {
        super(tooltip);
    }

    @Override
    protected Component getDamageComponent() {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);

        // TODO GunInfo

        if (cap != null && cap.getEnergyStored() > 0) {
//            double damage = (GunsTool.getGunDoubleTag(stack, "Damage", 0) +
//                    GunsTool.getGunDoubleTag(stack, "ChargedDamage", 0))
//                    * TooltipTool.perkDamage(stack);
//            return Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
//                    .append(Component.literal("").withStyle(ChatFormatting.RESET))
//                    .append(Component.literal(FormatTool.format1D(damage) + (TooltipTool.heBullet(stack) ? " + " +
//                                    FormatTool.format1D(0.8 * damage * (1 + 0.1 * TooltipTool.heBulletLevel(stack))) : ""))
//                            .withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD));
        } else {
//            double damage = GunsTool.getGunDoubleTag(stack, "Damage", 0) * TooltipTool.perkDamage(stack);
//            return Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
//                    .append(Component.literal("").withStyle(ChatFormatting.RESET))
//                    .append(Component.literal(FormatTool.format1D(damage) + (TooltipTool.heBullet(stack) ?
//                            FormatTool.format1D(0.4 * damage * (1 + 0.1 * TooltipTool.heBulletLevel(stack))) : "")).withStyle(ChatFormatting.GREEN));
        }
        return Component.literal("");
    }
}
