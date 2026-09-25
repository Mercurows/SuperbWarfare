package com.atsuishio.superbwarfare.item.attachment

import com.atsuishio.superbwarfare.client.tooltip.component.AttachmentImageComponent
import com.atsuishio.superbwarfare.item.gun.GunItem
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import java.util.*

/**
 * 副武器物品：**既是配件，又是一把真枪**。
 *
 * 同一物品 id 下同时拥有：
 * - `sbw/attachments/<id>.json` —— 配件定义（槽位、挂点、模型、`SubWeapon` 定义）；
 * - `sbw/guns/<id>.json` —— 枪数据（`GunData.getDefault()` 在 `defaultDataId` 为空时按物品注册 id 解析）。
 *
 * 所以 `SubWeaponInfo.Data` 是可选的：不写就用物品自身 id。
 *
 * **手持时必须按普通物品处理**：它只有装在正常枪械上才生效 ——
 * [useAsWeaponInHand] 返回 `false`，所有"手持边界"的门禁都问
 * [GunItem.isHeldWeapon] 而不是 `is GunItem`。
 *
 * **不带耐久条**：副武器的耐久写在主武器 NBT 的共享子 tag 上，不会触发主武器那种损坏事件；
 * 与其显示一条永远不掉的耐久条，不如直接不显示。
 */
open class SubWeaponItem @JvmOverloads constructor(
    override val attachmentId: String,
    rarity: Rarity = Rarity.COMMON,
) : GunItem(Properties().rarity(rarity)), AttachmentProvider {

    /** 手持时不是"枪"：不渲染枪身、不改视角、不进输入链路 */
    override fun useAsWeaponInHand(): Boolean = false

    /** 不带耐久条 */
    override fun getMaxDamage(stack: ItemStack): Int = 0

    override fun isDamageable(stack: ItemStack?): Boolean = false

    /** 物品提示走**配件**那套图，而不是枪械 tooltip */
    override fun getTooltipImage(stack: ItemStack): Optional<TooltipComponent> {
        return if (definition() == null) {
            Optional.empty()
        } else {
            Optional.of(AttachmentImageComponent(stack))
        }
    }
}
