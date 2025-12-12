package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.BooleanValue
import com.atsuishio.superbwarfare.data.gun.value.Timer

class Bolt(data: GunData) {

    @JvmField
    val needed: BooleanValue = BooleanValue(data.data(), "NeedBoltAction", false)

    @JvmField
    val actionTimer: Timer = Timer(data.data(), "BoltActionTime")
}
