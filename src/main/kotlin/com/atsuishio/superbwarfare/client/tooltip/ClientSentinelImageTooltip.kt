package com.atsuishio.superbwarfare.client.tooltip

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraftforge.common.capabilities.ForgeCapabilities

class ClientSentinelImageTooltip(tooltip: GunImageComponent) : ClientGunImageTooltip(tooltip) {
    override val damageComponent: Component
        get() {
            val energy = stack.getCapability(ForgeCapabilities.ENERGY)
                .map { it.energyStored }.orElse(0)

            if (energy > 0) {
                val damage = data.get(GunProp.DAMAGE)
                val explosionDamage = data.get(GunProp.EXPLOSION_DAMAGE)

                var dmgStr = format1D(damage)
                if (data.get(GunProp.PROJECTILE_AMOUNT) > 1) {
                    dmgStr = dmgStr + " * " + data.get(GunProp.PROJECTILE_AMOUNT)
                }

                var component =
                    Component.translatable("des.superbwarfare.guns.damage")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.empty().withStyle(ChatFormatting.RESET))
                        .append(
                            Component.literal(dmgStr).withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD)
                        )

                if (explosionDamage > 0) {
                    var expDmgStr = format1D(explosionDamage)
                    if (data.get(GunProp.PROJECTILE_AMOUNT) > 1) {
                        expDmgStr = expDmgStr + " * " + data.get(GunProp.PROJECTILE_AMOUNT)
                    }
                    component = component
                        .append(Component.empty().withStyle(ChatFormatting.RESET))
                        .append(
                            Component.literal(" + $expDmgStr")
                                .withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD)
                        )
                }
                return component
            } else {
                return super.damageComponent
            }
        }
}
