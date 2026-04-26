package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.Perk

object BrainStorm : Perk("brain_storm", Type.DAMAGE) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) = with(GunProp) {
        modifier[HEADSHOT] += 0.25 * modifier.data.perk.getLevel(this@BrainStorm)
    }
}
