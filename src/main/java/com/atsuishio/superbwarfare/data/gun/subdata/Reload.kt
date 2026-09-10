package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.*

/**
 * Reload state.
 *
 * Backed by [com.atsuishio.superbwarfare.data.gun.GunState] instead of the gun tag: every field keeps
 * its old type and name, but reads come from the immutable state snapshot and writes persist through
 * `GunData.update`.
 */
class Reload(private val gun: GunData) {

    @JvmField
    val reloadTimer: Timer = StateTimer(
        gun, { it.reloadTime }, { state, value -> state.copy(reloadTime = value) }, "Reload"
    )

    @JvmField
    val totalTicks: IntValue = StateIntValue(
        gun, { it.reloadTotalTime }, { state, value -> state.copy(reloadTotalTime = value) }
    )

    @JvmField
    val prepareTimer: Timer = StateTimer(
        gun, { it.prepareTime }, { state, value -> state.copy(prepareTime = value) }, "Prepare"
    )

    @JvmField
    val prepareLoadTimer: Timer = StateTimer(
        gun, { it.prepareLoadTime }, { state, value -> state.copy(prepareLoadTime = value) }, "PrepareLoad"
    )

    @JvmField
    val iterativeLoadTimer: Timer = StateTimer(
        gun, { it.iterativeLoadTime }, { state, value -> state.copy(iterativeLoadTime = value) }, "IterativeLoad"
    )

    @JvmField
    val finishTimer: Timer = StateTimer(
        gun, { it.finishTime }, { state, value -> state.copy(finishTime = value) }, "Finish"
    )

    @JvmField
    val finishTotalTicks: IntValue = StateIntValue(
        gun, { it.reloadFinishTotalTime }, { state, value -> state.copy(reloadFinishTotalTime = value) }
    )

    @JvmField
    val reloadStarter: Starter = StateStarter(
        gun, { it.startReload }, { state, value -> state.copy(startReload = value) }, "Reload"
    )

    @JvmField
    val singleReloadStarter: Starter = StateStarter(
        gun, { it.startSingleReload }, { state, value -> state.copy(startSingleReload = value) }, "SingleReload"
    )

    @JvmField
    val stage3Starter: Starter = StateStarter(
        gun, { it.startStage3Forcefully }, { state, value -> state.copy(startStage3Forcefully = value) },
        "Stage3Forcefully"
    )

    fun state() = when (gun.state.reloadState) {
        1 -> ReloadState.NORMAL_RELOADING
        2 -> ReloadState.EMPTY_RELOADING
        else -> ReloadState.NOT_RELOADING
    }

    fun normal() = state() == ReloadState.NORMAL_RELOADING

    fun empty() = state() == ReloadState.EMPTY_RELOADING

    fun setState(state: ReloadState) {
        // NOT_RELOADING is the field default, so the key disappears exactly like the old remove().
        val value = if (state == ReloadState.NOT_RELOADING) 0 else state.ordinal
        if (gun.state.reloadState == value) return
        gun.update { it.copy(reloadState = value) }
    }

    val stage: IntValue = StateIntValue(
        gun, { it.reloadStage }, { state, value -> state.copy(reloadStage = value) }
    )

    fun stage() = stage.get()

    fun setStage(stage: Int) {
        this.stage.set(stage)
    }

    fun time() = reloadTimer.get()

    fun setTime(time: Int) {
        totalTicks.set(time)
        reloadTimer.set(time)
    }

    fun reduce() {
        reloadTimer.reduce()
    }

    fun currentProgress(): Float = progress(reloadTimer.get())

    fun previousProgress(): Float = progress(reloadTimer.get() + 1)

    fun setFinishTime(time: Int) {
        finishTotalTicks.set(time)
        finishTimer.set(time)
    }

    fun finishCurrentProgress(): Float = finishProgress(finishTimer.get())

    fun finishPreviousProgress(): Float = finishProgress(finishTimer.get() + 1)

    private fun progress(remaining: Int): Float {
        val total = totalTicks.get()
        if (total <= 0) return 0f
        return 1f - remaining.toFloat() / total.toFloat()
    }

    private fun finishProgress(remaining: Int): Float {
        val total = finishTotalTicks.get()
        if (total <= 0) return 0f
        return 1f - remaining.toFloat() / total.toFloat()
    }
}
