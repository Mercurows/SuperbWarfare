package com.atsuishio.superbwarfare.data.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GunActionTimeline {
    @SerialName("RELOAD")
    RELOAD,

    @SerialName("RELOAD_NORMAL")
    RELOAD_NORMAL,

    @SerialName("RELOAD_EMPTY")
    RELOAD_EMPTY,

    /**
     * 弹鼓版的时间线，当前弹匣等级属于枪械的 `DrumLevels` 时生效，用于换弹动画/时长与普通弹匣不同、
     * 无法共用同一组 `Progress` 的枪械（见 [isDrumLevel]）。
     *
     * 某一组时间线只要配了 `_DRUM` 条目，弹鼓状态下就只认 `_DRUM`；没配则回退到普通条目。
     */
    @SerialName("RELOAD_NORMAL_DRUM")
    RELOAD_NORMAL_DRUM,

    /** [RELOAD_NORMAL_DRUM] 的空仓版本，见 [RELOAD_NORMAL_DRUM]。 */
    @SerialName("RELOAD_EMPTY_DRUM")
    RELOAD_EMPTY_DRUM,

    @SerialName("RELOAD_FINISH")
    RELOAD_FINISH,

    /**
     * 逐发换弹的 `PrepareLoad` 阶段，即在准备阶段就先装填一发（如 M870、M1897）。
     *
     * 只有配置了 `PrepareLoadTime` 且空仓装填的枪械才会进入该阶段，见
     * `GunEventHandler.handleGunSingleReload`。
     */
    @SerialName("RELOAD_PREPARE_LOAD")
    RELOAD_PREPARE_LOAD,

    @SerialName("NO_AMMO")
    NO_AMMO,

    @SerialName("BOLT")
    BOLT,
}

@Serializable
enum class GunStateAction {
    @SerialName("HOLD_OPEN")
    HOLD_OPEN,

    @SerialName("CLOSE_STRIKE")
    CLOSE_STRIKE,

    @SerialName("CLOSE_HAMMER")
    CLOSE_HAMMER,

    @SerialName("EMPTY")
    EMPTY,

    @SerialName("HIDE_BULLET_CHAIN")
    HIDE_BULLET_CHAIN,
}

/**
 * Data-driven reload/bolt timeline action.
 *
 * [progress] is measured from the start of the selected timeline:
 * 0.0 means the action starts and 1.0 means the action is complete.
 */
@Serializable
data class GunActionStep(
    @SerialName("Timeline")
    val timeline: GunActionTimeline = GunActionTimeline.RELOAD,

    @SerialName("Progress")
    val progress: Float = 0f,

    @SerialName("Action")
    val action: GunStateAction = GunStateAction.HOLD_OPEN,

    @SerialName("Value")
    val value: Boolean = false,
) {
    fun apply(data: GunData) {
        when (action) {
            GunStateAction.HOLD_OPEN -> data.holdOpen.set(value)
            GunStateAction.CLOSE_STRIKE -> data.closeStrike.set(value)
            GunStateAction.CLOSE_HAMMER -> data.closeHammer.set(value)
            GunStateAction.EMPTY -> data.isEmpty.set(value)
            GunStateAction.HIDE_BULLET_CHAIN -> data.hideBulletChain.set(value)
        }
    }
}
