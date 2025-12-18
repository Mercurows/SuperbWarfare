package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

public class ClientSentinelImageTooltip extends ClientGunImageTooltip {

    public ClientSentinelImageTooltip(GunImageComponent tooltip) {
        super(tooltip);
    }

    @Override
    protected Component getDamageComponent() {
        int energy = stack.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::getEnergyStored).orElse(0);

        if (energy > 0) {
            var computed = data.compute();
            double damage = computed.damage;
            double explosionDamage = computed.explosionDamage;

            String dmgStr = FormatTool.format1D(damage);
            if (computed.projectileAmount > 1) {
                dmgStr = dmgStr + " * " + computed.projectileAmount;
            }

            var component = Component.translatable("des.superbwarfare.guns.damage").withStyle(ChatFormatting.GRAY)
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(Component.literal(dmgStr).withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD));

            if (explosionDamage > 0) {
                String expDmgStr = FormatTool.format1D(explosionDamage);
                if (computed.projectileAmount > 1) {
                    expDmgStr = expDmgStr + " * " + computed.projectileAmount;
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
