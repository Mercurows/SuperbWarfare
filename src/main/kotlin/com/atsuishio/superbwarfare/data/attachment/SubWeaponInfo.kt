package com.atsuishio.superbwarfare.data.attachment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 副武器定义（`AttachmentDefinition.SubWeapon`）。
 *
 * **能力式而不是槽位式**：任何槽位的配件只要带上这个 POJO 就算副武器，不需要新槽位类型。
 * 刺刀没有它，所以刺刀只是"改主武器近战动作"的配件。
 *
 * ```jsonc
 * // sbw/attachments/gp25.json
 * {
 *   "Slot": "SubWeapon",
 *   "SubWeapon": {
 *     "Data": null,             // 可选：默认 null = 用物品自身 id 对应的 sbw/guns/gp25.json
 *     "AmmoSlot": "SubWeapon",  // 副武器自己的弹药槽
 *     "Cooldown": 20            // 触发冷却；0 = 用 Data 里的 RPM 决定
 *   },
 *   "Model": "...", "Texture": "..."
 * }
 * ```
 *
 * @param data 枪数据 id 覆盖。为 `null`（默认）时按**物品注册 id** 解析
 *   （`SubWeaponItem` 本身就是 `GunItem`，`GunData.getDefault()` 在 `defaultDataId` 为空时
 *   会走 `item.getDefaultData(this)`），所以同一物品 id 下"配件定义 + 枪数据"成对出现即可。
 * @param ammoSlot 副武器自己的弹药槽名（默认 `SubWeapon`）。
 *   ⚠ 当前实现里副武器的弹匣就是**它自己合成栈上的 `data.ammo`** —— 副武器的状态全部住在
 *   主武器 NBT 里那个附件子 tag 上，与主武器天然隔离，所以这个字段目前只影响
 *   "切换弹种时弹药的搬运槽位"（`GunData` 里 `ammoSlot` 的唯一用途），不影响开火。
 * @param cooldown 触发冷却 tick；`0` = 用副武器数据的 RPM 算一个射击周期。
 *   写在**主武器**的冷却表上（键 `sub:<槽位>`），所以客户端能直接读到。
 * @param animation 副武器自带动画（二期路线，当前未使用；副武器暂时不做动画）
 */
@Serializable
data class SubWeaponInfo(
    @SerialName("Data")
    val data: String? = null,

    @SerialName("AmmoSlot")
    val ammoSlot: String = DEFAULT_AMMO_SLOT,

    @SerialName("Cooldown")
    val cooldown: Int = 0,

    @SerialName("Animation")
    val animation: String? = null,
) {
    companion object {
        /** 副武器默认的弹药槽名 */
        const val DEFAULT_AMMO_SLOT: String = "SubWeapon"
    }
}
