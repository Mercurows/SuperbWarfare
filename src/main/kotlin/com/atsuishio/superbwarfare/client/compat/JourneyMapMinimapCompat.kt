package com.atsuishio.superbwarfare.client.compat

import com.atsuishio.superbwarfare.Mod
import journeymap.api.v2.client.IClientAPI
import journeymap.api.v2.client.IClientPlugin
import journeymap.api.v2.common.JourneyMapPlugin

/**
 * Soft JourneyMap integration that hides its minimap while the local player is in SBW vehicle.
 *
 * JourneyMap discovers [JourneyMapMinimapPlugin] only when it is installed. The API is compile-only,
 * so this class is never initialized in installations without JourneyMap.
 */
object JourneyMapMinimapCompat {
    private var api: IClientAPI? = null
    private var vehicleMinimapOverride = false
    private var minimapWasEnabled = false

    internal fun initialize(clientApi: IClientAPI) {
        api = clientApi
    }

    /** Synchronizes JourneyMap's minimap with whether the local player is riding SBW vehicle. */
    fun update(ridingVehicle: Boolean) {
        val clientApi = api ?: return

        if (ridingVehicle) {
            if (!vehicleMinimapOverride) {
                vehicleMinimapOverride = true
                minimapWasEnabled = clientApi.minimapEnabled()
            }
            if (clientApi.minimapEnabled()) {
                clientApi.toggleMinimap(false)
            }
            return
        }

        if (vehicleMinimapOverride && minimapWasEnabled) {
            clientApi.toggleMinimap(true)
        }
        vehicleMinimapOverride = false
        minimapWasEnabled = false
    }
}

/** JourneyMap entry point. It receives the API only when JourneyMap is present on the client. */
@JourneyMapPlugin(apiVersion = IClientAPI.API_VERSION)
class JourneyMapMinimapPlugin : IClientPlugin {
    override fun getModId(): String = Mod.MODID

    override fun initialize(jmClientApi: IClientAPI) {
        JourneyMapMinimapCompat.initialize(jmClientApi)
    }
}
