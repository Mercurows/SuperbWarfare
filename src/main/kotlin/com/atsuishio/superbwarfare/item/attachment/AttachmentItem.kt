package com.atsuishio.superbwarfare.item.attachment

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag

open class AttachmentItem @JvmOverloads constructor(
    private val attachmentId: String,
    rarity: Rarity = Rarity.COMMON
) : Item(Properties().rarity(rarity)) {

    open fun definition(): AttachmentDefinition? = AttachmentDefinition.from(attachmentId)

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        val definition = definition() ?: return
        tooltipComponents.add(
            Component.translatable("attachment.superbwarfare.slot")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(definition.slot.attachmentName))
        )
    }
}
