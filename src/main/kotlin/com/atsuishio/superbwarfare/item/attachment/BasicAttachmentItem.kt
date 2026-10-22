package com.atsuishio.superbwarfare.item.attachment

import com.atsuishio.superbwarfare.client.tooltip.component.AttachmentImageComponent
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import java.util.*

open class BasicAttachmentItem(
    override val attachmentId: String,
    properties: Properties,
) : Item(properties), AttachmentProvider {

    @JvmOverloads
    constructor(attachmentId: String, rarity: Rarity = Rarity.COMMON) :
            this(attachmentId, Properties().rarity(rarity))

    override fun getTooltipImage(stack: ItemStack): Optional<TooltipComponent> {
        return if (definition() == null) {
            Optional.empty()
        } else {
            Optional.of(AttachmentImageComponent(stack))
        }
    }
}
