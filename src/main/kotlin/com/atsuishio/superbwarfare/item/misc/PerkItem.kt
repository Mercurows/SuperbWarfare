package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.Perk
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.neoforged.neoforge.registries.DeferredHolder
import javax.annotation.ParametersAreNonnullByDefault

open class PerkItem<T : Perk>(private val perkSupplier: DeferredHolder<Perk, T>) : Item(Properties()) {
    val perk: Perk
        get() = this.perkSupplier.get()

    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        val perk = this.perk
        val chatFormatting = when (perk.type) {
            Perk.Type.AMMO -> ChatFormatting.YELLOW
            Perk.Type.FUNCTIONAL -> ChatFormatting.GREEN
            Perk.Type.DAMAGE -> ChatFormatting.RED
        }

        tooltipComponents.add(
            Component.translatable("des.superbwarfare." + perk.descriptionId).withStyle(ChatFormatting.GRAY)
        )
        tooltipComponents.add(Component.empty())
        tooltipComponents.add(
            Component.translatable("perk.superbwarfare.slot").withStyle(ChatFormatting.GOLD)
                .append(
                    Component.translatable("perk.superbwarfare.slot_" + perk.type.typeName)
                        .withStyle(chatFormatting)
                )
        )
        if (perk is AmmoPerk) {
            if (perk.damageRate < 1) {
                tooltipComponents.add(
                    Component.translatable("des.superbwarfare.perk_damage_reduce").withStyle(ChatFormatting.RED)
                )
            } else if (perk.damageRate > 1) {
                tooltipComponents.add(
                    Component.translatable("des.superbwarfare.perk_damage_plus").withStyle(ChatFormatting.GREEN)
                )
            }

            if (perk.speedRate < 1) {
                tooltipComponents.add(
                    Component.translatable("des.superbwarfare.perk_speed_reduce").withStyle(ChatFormatting.RED)
                )
            } else if (perk.speedRate > 1) {
                tooltipComponents.add(
                    Component.translatable("des.superbwarfare.perk_speed_plus").withStyle(ChatFormatting.GREEN)
                )
            }

            if (perk.slug) {
                tooltipComponents.add(
                    Component.translatable("des.superbwarfare.perk_slug").withStyle(ChatFormatting.YELLOW)
                )
            }
        }
    }
}