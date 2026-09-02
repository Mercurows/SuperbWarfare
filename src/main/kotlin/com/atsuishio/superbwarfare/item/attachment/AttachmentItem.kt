package com.atsuishio.superbwarfare.item.attachment

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

open class AttachmentItem @JvmOverloads constructor(
    private val attachmentId: String,
    rarity: Rarity = Rarity.COMMON
) : Item(Properties().rarity(rarity)) {

    open fun definition(): AttachmentDefinition? = AttachmentDefinition.from(attachmentId)

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltips: MutableList<Component>,
        isAdvanced: TooltipFlag,
    ) {
        val definition = definition() ?: return
        tooltips.add(
            Component.translatable("attachment.superbwarfare.slot")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(definition.slot.attachmentName))
        )
    }
}
