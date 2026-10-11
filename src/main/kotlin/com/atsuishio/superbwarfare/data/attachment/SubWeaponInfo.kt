package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.SingleOrList
import com.atsuishio.superbwarfare.data.attachment.SubWeaponInfo.Companion.DEFAULT_FIRE_ANIMATION
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 副武器定义（`AttachmentDefinition.SubWeapon`）
 *
 * 按能力而不是按槽位划分：任何槽位的配件只要带上这个 POJO 就算副武器，刺刀没有它，
 * 所以刺刀只是"改主武器近战动作"的配件
 *
 * ```jsonc
 * // sbw/attachments/sub_weapon_gp_25.json
 * {
 *   "Slot": "SubWeapon",
 *   "Bone": "sub_weapon_pos",
 *   "SubWeapon": {
 *     "Data": null,                        // 可选：默认用附件自身 id 对应的 sbw/guns/sub_weapon_gp_25.json
 *     "AmmoSlot": "SubWeapon",
 *     "Animation": ["fire_sub_weapon"],    // 可选：副武器激活时宿主枪的开火动画候选链
 *     "ReloadSound": "...", "ReloadEndSound": "..."
 *   },
 *   "Model": "...", "Texture": "..."
 * }
 * ```
 *
 * 副武器是独立的 `GunData`，因此换弹动画写在它自己的枪械资源里（`sbw/guns/<id>.json` 的 `Animation.Reload`），
 * 换弹之外它就是静止挂在枪上，持枪态以主武器的 `idle` 为准
 *
 * @param data 枪数据 id 覆盖（`sbw/guns/<path>.json` 的 id，带命名空间），为 null 时按附件自己的注册 id 解析，
 *   所以同一物品 id 下"配件定义 + 枪数据"成对出现即可，写上它就是多个配件 id 共用同一份枪数据，
 *   与手持形态那把武器共用同一份 json 通常不是好主意：手持数据里有很多只对持枪流程有意义的字段
 * @param ammoSlot 副武器自己的弹药槽名（默认 `SubWeapon`），副武器的状态住在主武器 NBT 的附件子 tag 上，
 *   与主武器天然隔离，所以这个字段只影响切换弹种时弹药的搬运槽位，不影响开火
 * @param animation 副武器激活时**宿主枪**使用的开火动画候选链（写字符串 = 单候选，写列表 = 按顺序取第一个存在的 clip），
 *   解析规则与 `MeleeAction.Animation` 一致（见 `GunAnimationNames`）：以 `animation.` 开头当全名，
 *   否则按宿主枪 id 拼成 `animation.<枪 id>.<短名>`，候选全落空时退回宿主枪自己的 `GunAnimation.Fire`，
 *   不写时的默认值见 [DEFAULT_FIRE_ANIMATION]，写 `[]` 表示不要候选、直接用 `Fire`
 * @param reloadSound 换弹开始音效（状态跳变时由 `playLocalSound` 播给射手），现在是服务端/无动画时的兜底：
 *   如果换弹动画的 `sound_effects` 里也有同名关键帧，两边都写就会响两遍
 * @param reloadEndSound 换弹完成音效，同上
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
    /** 副武器激活时宿主枪实际使用的开火动画候选链，字段不写就用 [DEFAULT_FIRE_ANIMATION] */
    fun fireAnimationCandidates(): List<String> = animation?.list ?: DEFAULT_FIRE_ANIMATION

    /** 候选链是不是数据里显式写的，默认候选落空属正常情况，显式写的落空才需要报错 */
    val hasExplicitFireAnimation: Boolean get() = animation != null

    companion object {
        /** 副武器默认的弹药槽名 */
        const val DEFAULT_AMMO_SLOT: String = "SubWeapon"

        /**
         * 副武器机瞄位形的约定骨骼名（在附件模型里找它），没有配置字段
         *
         * 与 `GeoGunRenderer.IRON_VIEW_BONE` 同名，附件模型里没有这支骨骼时回退宿主枪的
         * `scope_view` / `iron_view`
         */
        const val VIEW_BONE: String = "iron_view"

        /**
         * 副武器持枪位形（不瞄准时相机坐在哪里）的约定骨骼名，同样没有配置字段
         *
         * 与 `GeoGunRenderer.IDLE_VIEW_BONE` 同名，部署期间用它取代主武器的 `idle_view`，
         * 附件模型里没有就继续用主武器的
         */
        const val IDLE_VIEW_BONE: String = "idle_view"

        /** 不写 `Animation` 时的默认候选链：只试 `fire_sub_weapon`，落空则退回宿主枪的 `Fire` */
        @JvmField
        val DEFAULT_FIRE_ANIMATION: List<String> = listOf("fire_sub_weapon")
    }
}
