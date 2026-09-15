package com.atsuishio.superbwarfare.item.attachment

import com.atsuishio.superbwarfare.client.tooltip.component.AttachmentImageComponent
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import java.util.*

open class AttachmentItem @JvmOverloads constructor(
    private val attachmentId: String,
    rarity: Rarity = Rarity.COMMON
) : Item(Properties().rarity(rarity)) {

    open fun definition(): AttachmentDefinition? = AttachmentDefinition.from(attachmentId)

    override fun getTooltipImage(stack: ItemStack): Optional<TooltipComponent> {
        return if (definition() == null) {
            Optional.empty()
        } else {
            Optional.of(AttachmentImageComponent(stack))
        }
    }
}
