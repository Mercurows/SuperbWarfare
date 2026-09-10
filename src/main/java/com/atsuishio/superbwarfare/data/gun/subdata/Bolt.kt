package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.StateBooleanValue
import com.atsuishio.superbwarfare.data.gun.value.StateIntValue
import com.atsuishio.superbwarfare.data.gun.value.StateTimer
import com.atsuishio.superbwarfare.data.gun.value.Timer

/**
 * Bolt-action state.
 *
 * Backed by [com.atsuishio.superbwarfare.data.gun.GunState] instead of reading the gun tag directly:
 * the accessors keep their old types (so callers and compiled code are unaffected) but every write goes
 * through `GunData.update`, which lands in the gun stack's tag at once, so no caller has to remember a
 * `save()`.
 */
class Bolt(gun: GunData) {

    @JvmField
    val needed = StateBooleanValue(
        gun, { it.needBoltAction }, { state, value -> state.copy(needBoltAction = value) }
    )

    @JvmField
    val actionTimer: Timer = StateTimer(
        gun, { it.boltActionTime }, { state, value -> state.copy(boltActionTime = value) }, "BoltActionTime"
    )

    @JvmField
    val totalTicks = StateIntValue(
        gun, { it.boltActionTotalTime }, { state, value -> state.copy(boltActionTotalTime = value) }
    )

    fun start(total: Int) {
        actionTimer.set(total)
        totalTicks.set(total)
    }

    fun currentProgress(): Float = progress(actionTimer.get())

    fun previousProgress(): Float = progress(actionTimer.get() + 1)

    private fun progress(remaining: Int): Float {
        val total = totalTicks.get()
        if (total <= 0) return 0f
        return 1f - remaining.toFloat() / total.toFloat()
    }
}
