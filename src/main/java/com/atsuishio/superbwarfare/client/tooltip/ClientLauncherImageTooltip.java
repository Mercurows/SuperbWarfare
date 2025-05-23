package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.perk.Perk;
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

        for (var type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                damage = instance.perk().getDisplayDamage(damage, data, instance);
            }
        }

        double explosionDamage = data.explosionDamage();

        return Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(Component.literal(FormatTool.format1D(damage)).withStyle(ChatFormatting.GREEN)
                        .append(Component.empty().withStyle(ChatFormatting.RESET))
                        .append(Component.literal(" + " + FormatTool.format1D(explosionDamage)).withStyle(ChatFormatting.GOLD)));
    }
}
