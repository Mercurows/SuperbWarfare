package com.atsuishio.superbwarfare.capability.sync

import com.atsuishio.superbwarfare.capability.ModCapabilities
import com.atsuishio.superbwarfare.capability.entity.InfiniteAmmoCapability
import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import kotlin.jvm.optionals.getOrNull

/**
 * 所有参与自动同步的 capability 的登记入口。
 *
 * 新增一个自动同步的 capability 时，在这里加一行 [CapabilitySync.register]，
 * 并在其写入点调用 [CapabilitySync.markDirty] 即可，无需再写同步包。
 */
object ModSyncedCapabilities {

    /** 在 [net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent] 中调用一次 */
    fun register() {
        CapabilitySync.register(InfiniteAmmoCapability.ID) { entity ->
            entity.getCapability(ModCapabilities.INFINITE_AMMO_CAPABILITY).resolve().getOrNull()
        }

        CapabilitySync.register(PhosphorusFireCapability.ID) { entity ->
            entity.getCapability(ModCapabilities.PHOSPHORUS_FIRE_CAPABILITY).resolve().getOrNull()
        }

        CapabilitySync.register(PlayerVariable.ID) { entity ->
            entity.getCapability(ModCapabilities.PLAYER_VARIABLE).resolve().getOrNull()
        }
    }
}
