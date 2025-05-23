package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class ClientBocekImageTooltip extends ClientGunImageTooltip {

    public ClientBocekImageTooltip(GunImageComponent tooltip) {
        super(tooltip);
    }

    @Override
    protected Component getDamageComponent() {
        boolean slug = false;

        var data = GunData.from(stack);
        var perk = data.perk.get(Perk.Type.AMMO);
        if (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug) {
            slug = true;
        }

        double damage = data.damage();

        if (slug) {
            return super.getDamageComponent();
        } else {
            double shotDamage = damage * 0.1;
            double extraDamage = -1;
            for (var type : Perk.Type.values()) {
                var instance = data.perk.getInstance(type);
                if (instance != null) {
                    shotDamage = instance.perk().getDisplayDamage(shotDamage, data, instance);
                    if (instance.perk().getExtraDisplayDamage(shotDamage, data, instance) >= 0) {
                        extraDamage = instance.perk().getExtraDisplayDamage(shotDamage, data, instance);
                    }
                }
            }

            return Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(Component.literal(extraDamage >= 0 ? ("(" + FormatTool.format1D(shotDamage) + " + " + FormatTool.format1D(extraDamage) + ") * 10")
                                    : FormatTool.format1D(shotDamage, " * 10"))
                            .withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" / ").withStyle(ChatFormatting.RESET))
                    .append(Component.literal(FormatTool.format1D(shotDamage)).withStyle(ChatFormatting.GREEN));
        }
    }
}
