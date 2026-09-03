package com.atsuishio.superbwarfare.resource.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GunAnimation {
    // This should NOT be null or empty!
    @JvmField
    @SerialName("Idle")
    var idle: String? = null

    @JvmField
    @SerialName("Fire")
    var fire: String? = null

    @JvmField
    @SerialName("ChangeFireMode")
    var changeFireMode: String? = null

    @JvmField
    @SerialName("FireModes")
    var fireModes: List<String> = emptyList()

    // Reload > ReloadNormal | ReloadEmpty
    @JvmField
    @SerialName("Reload")
    var reload: String? = null

    @JvmField
    @SerialName("ReloadNormal")
    var reloadNormal: String? = null

    @JvmField
    @SerialName("ReloadEmpty")
    var reloadEmpty: String? = null

    @JvmField
    @SerialName("ReloadNormalDrum")
    var reloadNormalDrum: String? = null

    @JvmField
    @SerialName("ReloadEmptyDrum")
    var reloadEmptyDrum: String? = null

    @JvmField
    @SerialName("HoldOpen")
    var holdOpen: String? = null

    @JvmField
    @SerialName("CloseStrike")
    var closeStrike: String? = null

    @JvmField
    @SerialName("Prepare")
    var prepare: String? = null

    @JvmField
    @SerialName("Iterative")
    var iterative: String? = null

    @JvmField
    @SerialName("Finish")
    var finish: String? = null

    @JvmField
    @SerialName("Edit")
    var edit: String? = null

    @JvmField
    @SerialName("Bolt")
    var bolt: String? = null

    @JvmField
    @SerialName("Run")
    var run: String? = null

    @JvmField
    @SerialName("Melee")
    var melee: String? = null
}
