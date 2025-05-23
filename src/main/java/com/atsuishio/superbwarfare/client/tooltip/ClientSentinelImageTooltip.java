package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.capabilities.Capabilities;

public class ClientSentinelImageTooltip extends ClientEnergyImageTooltip {

    public ClientSentinelImageTooltip(GunImageComponent tooltip) {
        super(tooltip);
    }

    @Override
    protected Component getDamageComponent() {
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);

        if (cap != null && cap.getEnergyStored() > 0) {
            double damage = data.damage();
            double extraDamage = -1;
            for (var type : Perk.Type.values()) {
                var instance = data.perk.getInstance(type);
                if (instance != null) {
                    damage = instance.perk().getDisplayDamage(damage, data, instance);
                    if (instance.perk().getExtraDisplayDamage(damage, data, instance) >= 0) {
                        extraDamage = instance.perk().getExtraDisplayDamage(damage, data, instance);
                    }
                }
            }
            return Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(Component.literal(FormatTool.format1D(damage) + (extraDamage >= 0 ? " + " + FormatTool.format1D(extraDamage) : ""))
                            .withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD));
        } else {
            return super.getDamageComponent();
        }
    }
}
