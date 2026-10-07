package com.atsuishio.superbwarfare.resource.gun

import com.atsuishio.superbwarfare.data.SingleOrList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GunAnimation {
    // This should NOT be null or empty!
    @JvmField
    @SerialName("Idle")
    var idle: String? = null

    @JvmField
    @SerialName("Fire")
    var fire: String? = null

    @JvmField
    @SerialName("ChangeFireMode")
    var changeFireMode: String? = null

    @JvmField
    @SerialName("FireModes")
    var fireModes: List<String> = emptyList()

    // Reload > ReloadNormal | ReloadEmpty
    @JvmField
    @SerialName("Reload")
    var reload: String? = null

    @JvmField
    @SerialName("ReloadNormal")
    var reloadNormal: String? = null

    @JvmField
    @SerialName("ReloadEmpty")
    var reloadEmpty: String? = null

    @JvmField
    @SerialName("ReloadNormalDrum")
    var reloadNormalDrum: String? = null

    @JvmField
    @SerialName("ReloadEmptyDrum")
    var reloadEmptyDrum: String? = null

    @JvmField
    @SerialName("HoldOpen")
    var holdOpen: String? = null

    @JvmField
    @SerialName("CloseStrike")
    var closeStrike: String? = null

    // 加特林式枪管旋转
    @JvmField
    @SerialName("Hold")
    var hold: String? = null

    // 循环开火动画
    @JvmField
    @SerialName("FireLoop")
    var fireLoop: String? = null

    /** [fireLoop] 的淡入时长，单位秒 */
    @JvmField
    @SerialName("FireLoopFadeIn")
    var fireLoopFadeIn: Float = 0f

    /** [fireLoop] 的淡出时长，单位**秒**。松手之后这一层按它平滑退回当前姿态 */
    @JvmField
    @SerialName("FireLoopFadeOut")
    var fireLoopFadeOut: Float = 0f

    @JvmField
    @SerialName("Prepare")
    var prepare: String? = null

    @JvmField
    @SerialName("PrepareLoad")
    var prepareLoad: String? = null

    @JvmField
    @SerialName("Iterative")
    var iterative: String? = null

    @JvmField
    @SerialName("Finish")
    var finish: String? = null

    @JvmField
    @SerialName("Edit")
    var edit: String? = null

    @JvmField
    @SerialName("Bolt")
    var bolt: String? = null

    @JvmField
    @SerialName("Run")
    var run: String? = null

    // 近战动画 clip 名
    @JvmField
    @SerialName("Melee")
    var melee: SingleOrList<String>? = null

    /** 第一支近战 clip 名；没配或配成空列表时返回 `null`（老 GeckoLib 路径用） */
    fun firstMeleeName(): String? = melee?.list?.firstOrNull()

    /**
     * 换弹 clip 名
     */
    fun reloadClip(emptyReload: Boolean = true, drumLevel: Boolean = false): String? {
        if (!emptyReload) {
            if (drumLevel) reloadNormalDrum?.let { return it }
            return reloadNormal
        }
        if (drumLevel) reloadEmptyDrum?.let { return it }
        return reloadEmpty ?: reload
    }

    /*
     * TODO(V2 render migration):
     * These fields are intentionally data-only for now. QL1031/BOCEK still use their
     * legacy GeckoLib controllers, so do not wire them into rendering yet.
     *
     * After their V2 migration, GeoGunAnimationInstance should add a CHARGE state and:
     * 1. Select it when selectedFireModeInfo().isChargeMode() and charge is active.
     * 2. Play chargeCancel when an unfinished HOLD charge is cancelled.
     * 3. Keep CHARGE looping/holding at full for CHARGE mode.
     */
    @JvmField
    @SerialName("Charge")
    var charge: String? = null
}
