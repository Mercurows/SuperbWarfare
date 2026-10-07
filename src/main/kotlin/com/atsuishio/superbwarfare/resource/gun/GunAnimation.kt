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

    /**
     * 加特林式枪管旋转：按住开火键（加特林开镜也算）时把这一支循环叠加上去，播完不摘，只改播放速度。
     *
     * 转速按实际射速缩放（默认 1200RPM 是 1×），并**按这把枪的蓄力时长缓入缓出**：按住时从 0 平滑升到
     * 满速，松手后再平滑降回 0，降到 0 就停在那个角度不回位。详见 `GeoGunAnimationInstance.updateSpinRunner`。
     *
     * 不配的枪不参与。
     */
    @JvmField
    @SerialName("Hold")
    var hold: String? = null

    /**
     * 循环开火动画：**按住开火键期间一直循环**的那一层（枪口火焰、连射线束这类"开着火就没停过"的东西）。
     *
     * 与 [fire] **不互斥**：两者同帧生效，各管各的骨骼。[fire] 是叠加层（加在状态机之上）；
     * 这一层则是**按 [fireLoopFadeIn] / [fireLoopFadeOut] 从当前合成姿态交叉淡入淡出**
     * （见 `GeoGunAnimationInstance.updateFireLoopRunner`）——
     * 淡入就是"从现在的姿态插值到这支 clip"，所以**淡入的起点由状态动画自己给**：
     * idle 里把某块骨骼压成 0（修理工具的 `flame_illuminated: scale 0`）正好成为淡入的起点，
     * 不需要为了这一层另写一份"姿势为 0"的数据。
     *
     * 这一层**只在第一人称的本地玩家手里**有意义：它是"按住扳机"驱动的，3P 的别人看不到，
     * 而 V2 本来也只在 1P 应用姿态。
     *
     * 不配的枪不参与。
     */
    @JvmField
    @SerialName("FireLoop")
    var fireLoop: String? = null

    /**
     * [fireLoop] 的淡入时长，单位**秒**（与 `animation_length` 同一口径，`0.1` = 2 tick）。`0` = 立刻满权重。
     *
     * ⚠ 注意与 `sbw/guns/<id>.json` 里别的时长（`ShootDelay`、`NormalReloadTime` 这些）**不同单位**：
     * 那些是 tick，这两个是**秒** —— 它们和动画片长是一套时间，跟着动画文件走。
     */
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

    /**
     * 近战动画 clip 名。
     *
     * 写成**字符串**即单段（旧数据零改动）；写成**列表**就是一串可循环的近战 clip，
     * 下标由 `MeleeAction.Animation ?: melee[idx % size]` 决定。
     *
     * 注意动作表本身来自 `GunData`（PMC，按 stack），**不能**从 `GunResource` 取——
     * `GunResource` 是按物品注册 id 缓存的，配件/弹种覆盖看不到。
     */
    @JvmField
    @SerialName("Melee")
    var melee: SingleOrList<String>? = null

    /** 第一支近战 clip 名；没配或配成空列表时返回 `null`（老 GeckoLib 路径用） */
    fun firstMeleeName(): String? = melee?.list?.firstOrNull()

    /**
     * 换弹 clip 名的**唯一解析口径**。
     *
     * 为什么要有这个方法：这套优先级原先在主武器与副武器两条换弹动画链路上**各写了一份**，
     * 两份的兜底顺序还不一样 ——
     *
     * | 场景 | 顺序 | 位置 |
     * |---|---|---|
     * | 主武器 | `Reload` → 鼓式 → `ReloadNormal` / `ReloadEmpty` | `GeoGunAnimationInstance.resolveState` |
     * | 副武器 | `ReloadEmpty` → `Reload` | `GeoGunAnimationInstance.updateSubWeaponReload` |
     *
     * 主武器那份是"`Reload` 是通用兜底、`ReloadNormal`/`ReloadEmpty` 是细分"，副武器那份把
     * `ReloadEmpty` 提到了最前。好在**只配一个 `ReloadEmpty` 时两份结果相同**（GP-25 就是这种），
     * 所以这个不一致一直没有暴露。这里把副武器那份的口径抽成函数固定下来（**保持它的既有语义**，
     * 不趁机改行为），至少让"现在到底按哪条规则"只有一个答案。
     *
     * 注意它**只回答"数据里写的 clip 名"**，不回答"这支 clip 存不存在"——
     * 后者要问动画表（副武器问 `AttachmentModelReloadListener`）。
     *
     * @param emptyReload 这一次是不是空仓换弹（`data.reload.empty()`）；`false` → 走正常换弹那一支
     * @param drumLevel 是否鼓式弹匣（`GunData.isDrumLevel()`）
     */
    fun reloadClip(emptyReload: Boolean = true, drumLevel: Boolean = false): String? {
        // ⚠ 这里刻意**逐字保留**副武器原实现的两个分支，不顺手"修"成主武器那种更细的回退：
        // 主武器会在 `allowTacticalReload` 的枪上走 NORMAL_RELOADING（`GunEventHandler.startReload`），
        // 副武器的换弹同样可能落进那一支；把正常换弹的兜底顺序改掉就是在改我自己没验证过的路径。
        // 要动它，先让一把 `allowTacticalReload` 的副武器真的存在并实测。
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

    @JvmField
    @SerialName("ChargeCancel")
    var chargeCancel: String? = null
}
