package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.IAmmoStat
import com.atsuishio.superbwarfare.perk.Perk
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import java.util.function.Supplier

open class PerkItem(private val perkSupplier: Supplier<Perk>) : Item(Properties()) {
    val perk: Perk
        get() = this.perkSupplier.get()

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltips: MutableList<Component>,
        isAdvanced: TooltipFlag
    ) {
        val perk = this.perk
        val chatFormatting = when (perk.type) {
            Perk.Type.AMMO -> ChatFormatting.YELLOW
            Perk.Type.FUNCTIONAL -> ChatFormatting.GREEN
            Perk.Type.DAMAGE -> ChatFormatting.RED
        }

        tooltips.add(
            Component.translatable("des.superbwarfare." + perk.descriptionId).withStyle(ChatFormatting.GRAY)
        )
        tooltips.add(Component.empty())
        tooltips.add(
            Component.translatable("perk.superbwarfare.slot").withStyle(ChatFormatting.GOLD)
                .append(
                    Component.translatable("perk.superbwarfare.slot_" + perk.type.typeName)
                        .withStyle(chatFormatting)
                )
        )
        if (perk is AmmoPerk) {
            appendAmmoTooltips(perk.damageRate, perk.speedRate, perk.slug, tooltips)
        } else if (perk is IAmmoStat) {
            appendAmmoTooltips(perk.damageRate, perk.speedRate, perk.slug, tooltips)
        }
    }

    private fun appendAmmoTooltips(damageRate: Double, speedRate: Double, slug: Boolean, tooltips: MutableList<Component>) {
        if (damageRate < 1) {
            tooltips.add(
                Component.translatable("des.superbwarfare.perk_damage_reduce").withStyle(ChatFormatting.RED)
            )
        } else if (damageRate > 1) {
            tooltips.add(
                Component.translatable("des.superbwarfare.perk_damage_plus").withStyle(ChatFormatting.GREEN)
            )
        }

        if (speedRate < 1) {
            tooltips.add(
                Component.translatable("des.superbwarfare.perk_speed_reduce").withStyle(ChatFormatting.RED)
            )
        } else if (speedRate > 1) {
            tooltips.add(
                Component.translatable("des.superbwarfare.perk_speed_plus").withStyle(ChatFormatting.GREEN)
            )
        }

        if (slug) {
            tooltips.add(Component.translatable("des.superbwarfare.perk_slug").withStyle(ChatFormatting.YELLOW))
        }
    }
}
