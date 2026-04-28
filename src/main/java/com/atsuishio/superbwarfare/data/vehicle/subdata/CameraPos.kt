package com.atsuishio.superbwarfare.data.vehicle.subdata

import com.atsuishio.superbwarfare.data.StringOrVec3
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec2
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3

@Serializable
class CameraPos {
    @SerialName("Transform")
    var transform: String = "Default"

    @SerialName("Position")
    var position: SerializedVec3 = Vec3.ZERO

    @SerialName("Direction")
    var direction: StringOrVec3 = StringOrVec3("Default")

    @SerialName("ZoomPosition")
    var zoomPosition: SerializedVec3? = null

    @SerialName("ZoomDirection")
    var zoomDirection: StringOrVec3? = null

    @SerialName("UseFixedCameraPos")
    var useFixedCameraPos: Boolean = false

    @SerialName("UseSimulate3P")
    var useSimulate3P: Boolean = false

    @SerialName("Simulate3PPos")
    var simulate3PPos: SerializedVec2 = Vec2(6f, 1f)

    @SerialName("UseAircraftCamera")
    var useAircraftCamera: Boolean = false

    @SerialName("AircraftCameraPos")
    var aircraftCameraPos: SerializedVec3 = Vec3(0.0, 3.0, -10.0)
}
