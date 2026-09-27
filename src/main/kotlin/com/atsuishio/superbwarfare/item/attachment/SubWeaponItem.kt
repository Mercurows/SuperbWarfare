package com.atsuishio.superbwarfare.item.attachment

import com.atsuishio.superbwarfare.client.tooltip.component.AttachmentImageComponent
import com.atsuishio.superbwarfare.data.gun.GunData
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
 * 所以 `SubWeaponInfo.Data` 是可选的：不写就用物品自身 id（同名成对出现），
 * 写了就指向**别人那份**枪数据 —— 多个配件共用一个副武器数据时才需要它，
 * 与"手持形态的那把武器"共用一份 json 一般不是好主意（两份数据的关注点不同，
 * 详见 `SubWeaponInfo.data` 的说明）。落地见 `SubWeaponRuntime.applyBaselineId`。
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

    /**
     * **副武器不具有配件**（四期）：它是装到枪上的一个部件，不是一把可以被改装的枪 ——
     * 让下挂榴弹自己再挂一个握把没有意义。
     *
     * 于是改装界面（`GunItem.getItemScreen` → `canOpenEditScreen` + 这个谓词）对它不打开，
     * `/sbw attachment` 也只对**主手那把枪**生效（`AttachmentCommand.mainHandGunData`）。
     * 指令侧的兜底还有一条：`GunData.availableAttachments` 对副武器一律返回空表，
     * 所以补全不会列出候选、`canInstall` 也会直接失败。
     */
    override fun canEditAttachments(data: GunData) = false

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
