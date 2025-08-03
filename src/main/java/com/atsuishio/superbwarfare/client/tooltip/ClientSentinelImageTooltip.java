package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.FormatTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

public class ClientSentinelImageTooltip extends ClientEnergyImageTooltip {

    public ClientSentinelImageTooltip(GunImageComponent tooltip) {
        super(tooltip);
    }

    @Override
    protected Component getDamageComponent() {
        int energy = stack.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::getEnergyStored).orElse(0);

        if (energy > 0) {
            double damage = data.get(GunProp.DAMAGE);
            double extraDamage = -1;
            for (var type : Perk.Type.values()) {
                var instance = getGunData().perk.getInstance(type);
                if (instance != null) {
                    if (instance.perk().getExtraDisplayDamage(damage, getGunData(), instance) >= 0) {
                        extraDamage = instance.perk().getExtraDisplayDamage(damage, getGunData(), instance);
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
