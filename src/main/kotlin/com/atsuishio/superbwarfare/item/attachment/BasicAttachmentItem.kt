package com.atsuishio.superbwarfare.item.attachment

import com.atsuishio.superbwarfare.client.tooltip.component.AttachmentImageComponent
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import java.util.*

/**
 * 纯配件物品：**不能开火、也不能手持当武器用**，只能装到枪的某个槽位上
 * （原 `AttachmentItem`，v6 改名，并把"配件身份"抽到 [AttachmentProvider] 接口上）。
 *
 * 副武器（三期）不走这个类，而是 [com.atsuishio.superbwarfare.item.gun.GunItem] + [AttachmentProvider]。
 */
open class BasicAttachmentItem @JvmOverloads constructor(
    override val attachmentId: String,
    rarity: Rarity = Rarity.COMMON
) : Item(Properties().rarity(rarity)), AttachmentProvider {

    override fun getTooltipImage(stack: ItemStack): Optional<TooltipComponent> {
        return if (definition() == null) {
            Optional.empty()
        } else {
            Optional.of(AttachmentImageComponent(stack))
        }
    }
}
