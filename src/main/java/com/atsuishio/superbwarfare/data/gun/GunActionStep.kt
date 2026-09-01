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

    @SerialName("RELOAD_FINISH")
    RELOAD_FINISH,

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
