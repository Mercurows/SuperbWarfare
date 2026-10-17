package com.atsuishio.superbwarfare.data.vehicle.subdata

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

/**
 * One wheel or track station of a vehicle.
 *
 * [position] is the wheel centre in the vehicle's local frame, in the same
 * space as `OBB.Position` (X left, Y up, Z forward).
 *
 * [Side] groups contact/load reporting and drive commands. Every SUPPORT
 * wheel has its own contact, spring/damper and force application point. [Role] is
 * `DriveGuide` or `IdlerGuide` for the end wheels of a track. Guide wheels do
 * not contribute to the suspension-height calculation; only `Support` stations
 * define the side's spring plane.
 */
@Serializable
data class WheelInfo(
    @SerialName("Side")
    var side: WheelSide = WheelSide.LEFT,

    @SerialName("Role")
    var role: WheelRole = WheelRole.SUPPORT,

    @SerialName("Position")
    var position: SerializedVec3 = Vec3.ZERO,

    /** Wheel radius in blocks; also used as the vertical half extent. */
    @SerialName("Radius")
    var radius: Double = 0.4,

    /** Lateral half width of the wheel or track shoe. */
    @SerialName("Width")
    var width: Double = 0.3,
    /** Explicit side-local model wheel id; not the JSON array offset. */
    @SerialName("Index")
    var index: Int? = null,
    /** Contact envelope includes the belt/shoe below a TRACK road wheel. */
    @SerialName("ContactRadius")
    var contactRadius: Double? = null,
) {
    fun limit() {
        if (radius <= 0.0 || !radius.isFinite()) radius = 0.4
        if (width <= 0.0 || !width.isFinite()) width = 0.3
        radius = radius.coerceIn(0.05, 2.0)
        width = width.coerceIn(0.05, 2.0)
    }
}

@Serializable
enum class WheelSide {
    @SerialName("Left")
    LEFT,

    @SerialName("Right")
    RIGHT,
}

@Serializable
enum class WheelRole {
    @SerialName("Support")
    SUPPORT,

    /** Driven sprocket at the end of a track. */
    @SerialName("DriveGuide")
    DRIVE_GUIDE,

    /** Passive idler at the opposite end of a track. */
    @SerialName("IdlerGuide")
    IDLER_GUIDE,
}
