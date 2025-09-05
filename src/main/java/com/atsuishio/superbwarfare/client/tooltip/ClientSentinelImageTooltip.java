package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunProp;
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
            double damage = data.get(GunProp.DAMAGE);
            double explosionDamage = data.get(GunProp.EXPLOSION_DAMAGE);

            String dmgStr = FormatTool.format1D(damage);
            if (data.get(GunProp.PROJECTILE_AMOUNT) > 1) {
                dmgStr = dmgStr + " * " + data.get(GunProp.PROJECTILE_AMOUNT);
            }

            var component = Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(Component.literal(dmgStr).withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD));

            if (explosionDamage > 0) {
                String expDmgStr = FormatTool.format1D(explosionDamage);
                if (data.get(GunProp.PROJECTILE_AMOUNT) > 1) {
                    expDmgStr = expDmgStr + " * " + data.get(GunProp.PROJECTILE_AMOUNT);
                }
                component = component
                        .append(Component.empty().withStyle(ChatFormatting.RESET))
                        .append(Component.literal(" + " + expDmgStr).withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD));
            }
            return component;
        } else {
            return super.getDamageComponent();
        }
    }
}
