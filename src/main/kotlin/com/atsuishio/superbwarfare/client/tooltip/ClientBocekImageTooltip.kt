package com.atsuishio.superbwarfare.client.tooltip

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.IAmmoStat
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

class ClientBocekImageTooltip(tooltip: GunImageComponent) : ClientGunImageTooltip(tooltip) {
    override val damageComponent: Component
        get() {
            var slug = false

            val data = from(stack)
            val perk = data.perk.get(Perk.Type.AMMO)
            slug = when (perk) {
                is AmmoPerk -> perk.slug
                is IAmmoStat -> perk.slug
                else -> false
            }

            val damage = data.get(GunProp.DAMAGE)

            if (slug) {
                return super.damageComponent
            } else {
                val shotDamage = damage * 0.1
                val explosionDamage = data.get(GunProp.EXPLOSION_DAMAGE) * 0.1

                return Component.translatable("des.superbwarfare.guns.damage")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(
                        Component.literal(
                            if (explosionDamage > 0)
                                ("( ${format1D(shotDamage)} + ${format1D(explosionDamage)}) * 10")
                            else
                                format1D(shotDamage, " * 10")
                        ).withStyle(ChatFormatting.GREEN)
                    )
                    .append(Component.literal(" / ").withStyle(ChatFormatting.RESET))
                    .append(
                        Component.literal(format1D(damage))
                            .withStyle(ChatFormatting.GREEN)
                    )
            }
        }
}
