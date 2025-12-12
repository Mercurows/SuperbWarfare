package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.IntValue
import com.atsuishio.superbwarfare.data.gun.value.ReloadState
import com.atsuishio.superbwarfare.data.gun.value.Starter
import com.atsuishio.superbwarfare.data.gun.value.Timer

class Reload(data: GunData) {
    private val data = data.data()

    @JvmField
    val reloadTimer = Timer(this.data, "Reload")

    @JvmField
    val prepareTimer = Timer(this.data, "Prepare")

    @JvmField
    val prepareLoadTimer = Timer(this.data, "PrepareLoad")

    @JvmField
    val iterativeLoadTimer = Timer(this.data, "IterativeLoad")

    @JvmField
    val finishTimer = Timer(this.data, "Finish")

    @JvmField
    val reloadStarter = Starter(this.data, "Reload")

    @JvmField
    val singleReloadStarter = Starter(this.data, "SingleReload")

    @JvmField
    val stage3Starter = Starter(this.data, "Stage3Forcefully")

    fun state() = when (data.getInt("ReloadState")) {
        1 -> ReloadState.NORMAL_RELOADING
        2 -> ReloadState.EMPTY_RELOADING
        else -> ReloadState.NOT_RELOADING
    }

    fun normal() = state() == ReloadState.NORMAL_RELOADING

    fun empty() = state() == ReloadState.EMPTY_RELOADING

    fun setState(state: ReloadState) {
        if (state == ReloadState.NOT_RELOADING) {
            data.remove("ReloadState")
        } else {
            data.putInt("ReloadState", state.ordinal)
        }
    }

    val stage = IntValue(this.data, "ReloadStage", 0)

    fun stage() = stage.get()

    fun setStage(stage: Int) {
        this.stage.set(stage)
    }

    fun time() = reloadTimer.get()

    fun setTime(time: Int) {
        reloadTimer.set(time)
    }

    fun reduce() {
        reloadTimer.reduce()
    }
}
