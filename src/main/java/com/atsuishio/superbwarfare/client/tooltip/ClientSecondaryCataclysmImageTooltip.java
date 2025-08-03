package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class ClientSecondaryCataclysmImageTooltip extends ClientEnergyImageTooltip {

    public ClientSecondaryCataclysmImageTooltip(GunImageComponent tooltip) {
        super(tooltip);
    }

    @Override
    protected Component getDamageComponent() {
        double damage = data.get(GunProp.DAMAGE);
        double explosionDamage = data.get(GunProp.EXPLOSION_DAMAGE);

        return Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(Component.literal(FormatTool.format1D(damage)).withStyle(ChatFormatting.GREEN)
                        .append(Component.empty().withStyle(ChatFormatting.RESET))
                        .append(Component.literal(" + " + FormatTool.format1D(explosionDamage)).withStyle(ChatFormatting.GOLD)));
    }
}
