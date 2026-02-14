package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.Mod
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tiers
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraftforge.common.ForgeMod
import java.util.*

class CrowbarItem : SwordItem(Tiers.IRON, 2, -2f, Properties().durability(400)) {
    override fun getAttributeModifiers(
        slot: EquipmentSlot,
        stack: ItemStack?
    ): Multimap<Attribute, AttributeModifier> {
        var map = super.getDefaultAttributeModifiers(slot)
        val uuid = UUID(slot.toString().hashCode().toLong(), 0)
        if (slot == EquipmentSlot.MAINHAND) {
            map = HashMultimap.create<Attribute, AttributeModifier>(map)
            map.put(
                ForgeMod.BLOCK_REACH.get(),
                AttributeModifier(uuid, Mod.ATTRIBUTE_MODIFIER, 3.0, AttributeModifier.Operation.ADDITION)
            )
        }
        return map
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.crowbar").withStyle(ChatFormatting.GRAY))
        pTooltipComponents.add(Component.translatable("des.superbwarfare.crowbar_2").withStyle(ChatFormatting.GRAY))
    }
}