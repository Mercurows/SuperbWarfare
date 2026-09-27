package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.SingleOrList
import com.atsuishio.superbwarfare.data.attachment.SubWeaponInfo.Companion.DEFAULT_FIRE_ANIMATION
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 副武器定义（`AttachmentDefinition.SubWeapon`）。
 *
 * **能力式而不是槽位式**：任何槽位的配件只要带上这个 POJO 就算副武器，不需要新槽位类型。
 * 刺刀没有它，所以刺刀只是"改主武器近战动作"的配件。
 *
 * ```jsonc
 * // sbw/attachments/sub_weapon_gp_25.json
 * {
 *   "Slot": "SubWeapon",
 *   "Bone": "sub_weapon_pos",
 *   "SubWeapon": {
 *     "Data": null,                        // 可选：默认 null = 用物品自身 id 对应的 sbw/guns/sub_weapon_gp_25.json
 *     "AmmoSlot": "SubWeapon",             // 副武器自己的弹药槽
 *     "Animation": ["fire_sub_weapon"],    // 可选：副武器**激活时宿主枪**的开火动画候选链（不写就是这一条）
 *     "ReloadSound": "...", "ReloadEndSound": "..."
 *   },
 *   "Model": "...", "Texture": "..."
 * }
 * ```
 *
 * ⚠ **换弹动画不在这里**（四期）：副武器是独立的 `GunData`，因此天然有独立的 `GunResource` ——
 * 换弹动画写在**它自己的枪械资源**里（`sbw/guns/<id>.json` 的 `Animation.Reload`），
 * 由它自己的附件模型播（`AttachmentModelReloadListener` 的 `animPath` + `BedrockAttachmentModel.applyPose`）。
 * 详见设计文档 §9.8.7。
 *
 * ⚠ **副武器不做 idle**：换弹之外它就是静止挂在枪上，**持枪态以主武器的 `idle` 为准**。
 *
 * @param data 枪数据 id 覆盖（`sbw/guns/<path>.json` 的 id，带命名空间）。
 *   为 `null`（默认）时按**附件自己的注册 id** 解析
 *   （`SubWeaponItem` 本身就是 `GunItem`，`GunData.getDefault()` 在 `defaultDataId` 为空时
 *   会走 `item.getDefaultData(this)`），所以同一物品 id 下"配件定义 + 枪数据"成对出现即可。
 *   写上它就是**多对一**：多个配件 id 共用同一份副武器枪数据时不必各复制一份同名 json。
 *   ⚠ 与"手持形态的那把武器"共用同一份 json 一般**不是**好主意：手持武器数据里有大量只对
 *   手持流程有意义的字段（`DrawTime`/`ZoomTime`/`AvailablePerks`/`Icon`/`ProjectileBone`……），
 *   而副武器需要的 `RPM`/`ShootShake`/`ProjectileLife` 未必在内，改一边会静默改另一边。
 * @param ammoSlot 副武器自己的弹药槽名（默认 `SubWeapon`）。
 *   ⚠ 当前实现里副武器的弹匣就是**它自己合成栈上的 `data.ammo`** —— 副武器的状态全部住在
 *   主武器 NBT 里那个附件子 tag 上，与主武器天然隔离，所以这个字段目前只影响
 *   "切换弹种时弹药的搬运槽位"（`GunData` 里 `ammoSlot` 的唯一用途），不影响开火。
 * @param animation 副武器**激活时宿主枪**使用的开火动画候选链（`SingleOrList`：写字符串 = 单候选，
 *   写列表 = 按顺序取第一个存在的 clip）。解析规则与 `MeleeAction.Animation` **完全一致**
 *   （见 [com.atsuishio.superbwarfare.resource.gun.GunAnimationNames]）：以 `animation.` 开头当全名，
 *   否则按**宿主枪 id** 拼成 `animation.<枪 id>.<短名>`；候选全落空时退回宿主枪自己的
 *   `GunAnimation.Fire`。
 *
 *   于是"装了下挂榴弹的枪做了 `fire_sub_weapon` 就用它、没做就照常播 `fire`"由**一条数据**表达，
 *   配件数据一个字都不用管是哪把枪。不写时的默认值见 [DEFAULT_FIRE_ANIMATION]
 *   （写 `[]` 可以显式表示"不要候选、就用 `Fire`"）。
 * @param reloadSound 换弹**开始**音效。四期**仍然需要**：换弹动画现在归副武器自己的资源，
 *   而那条新增的附件动画播放链路**不接数据包的 `sound_effects` 关键帧** ——
 *   写在副武器动画里的音效不会响。所以由配件数据声明、在状态跳变时 `playLocalSound` 播给射手。
 * @param reloadEndSound 换弹**完成**音效，同上。
 */
@Serializable
data class SubWeaponInfo(
    @SerialName("Data")
    val data: String? = null,

    @SerialName("AmmoSlot")
    val ammoSlot: String = DEFAULT_AMMO_SLOT,

    @SerialName("Animation")
    val animation: SingleOrList<String>? = null,

    @SerialName("ReloadSound")
    val reloadSound: SerializedSoundEvent? = null,

    @SerialName("ReloadEndSound")
    val reloadEndSound: SerializedSoundEvent? = null,
) {
    /**
     * 副武器**激活时宿主枪**实际使用的开火动画候选链。
     *
     * 字段不写 → [DEFAULT_FIRE_ANIMATION]（"优先 `fire_sub_weapon`，没有就退回 `Fire`"）；
     * 显式写空列表 → 空候选（直接走 `Fire`）。
     */
    fun fireAnimationCandidates(): List<String> = animation?.list ?: DEFAULT_FIRE_ANIMATION

    /**
     * 候选链是不是**数据里显式写的**。
     *
     * 决定"候选全部落空"要不要报 error：默认候选（`fire_sub_weapon`）落空是**正常**情况 ——
     * 绝大多数枪就没做这支 clip，退回 `Fire` 本来就是设计的一部分；而显式写了却一支都对不上，
     * 那就是数据或者动画文件写错了，必须让人看见。
     */
    val hasExplicitFireAnimation: Boolean get() = animation != null

    companion object {
        /** 副武器默认的弹药槽名 */
        const val DEFAULT_AMMO_SLOT: String = "SubWeapon"

        /**
         * 副武器瞄具位形的**约定骨骼名**（在附件模型里找它）。
         *
         * 与 `GeoGunRenderer.IRON_VIEW_BONE` 同名 —— 副武器是"挂在枪上的另一把枪"，
         * 它的机瞄位形语义与普通枪完全一样。**没有配置字段**：附件模型里有这支骨骼就用它，
         * 没有就回退宿主枪的 `scope_view` / `iron_view`（§9.8.6）。
         */
        const val VIEW_BONE: String = "iron_view"

        /**
         * 不写 `Animation` 时的默认候选链：只试 `fire_sub_weapon`。
         *
         * 短名会按宿主枪 id 拼成 `animation.<枪 id>.fire_sub_weapon`，
         * 因此这一条默认值就够表达"这把枪做了副武器开火动画就用它，没做就照常播 `fire`"——
         * 宿主枪的 `GunAnimation.Fire` 是候选全落空后的兜底，不需要写进候选里。
         */
        @JvmField
        val DEFAULT_FIRE_ANIMATION: List<String> = listOf("fire_sub_weapon")
    }
}
