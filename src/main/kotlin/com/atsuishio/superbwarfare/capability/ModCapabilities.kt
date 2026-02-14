package com.atsuishio.superbwarfare.capability

import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.CapabilityToken

object ModCapabilities {
    @JvmField
    val PLAYER_VARIABLE: Capability<PlayerVariable> =
        CapabilityManager.get(object : CapabilityToken<PlayerVariable>() {})

    @JvmField
    val PHOSPHORUS_FIRE_CAPABILITY: Capability<PhosphorusFireCapability> =
        CapabilityManager.get(object : CapabilityToken<PhosphorusFireCapability>() {})
}
