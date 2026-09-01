package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.BooleanValue
import com.atsuishio.superbwarfare.data.gun.value.IntValue
import com.atsuishio.superbwarfare.data.gun.value.Timer

class Bolt(data: GunData) {

    @JvmField
    val needed: BooleanValue = BooleanValue(data.data(), "NeedBoltAction", false)

    @JvmField
    val actionTimer: Timer = Timer(data.data(), "BoltActionTime")

    @JvmField
    val totalTicks = IntValue(data.data(), "BoltActionTotalTime", 0)

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
