package com.atsuishio.superbwarfare.data.vehicle.subdata

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import com.atsuishio.superbwarfare.tools.OBB
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond

@Serializable
class OBBInfo {
    @SerialName("Size")
    var size: SerializedVec3 = Vec3.ZERO

    @SerialName("Position")
    var position: SerializedVec3 = Vec3.ZERO

    @SerialName("Transform")
    var transform: String? = "Default"

    @SerialName("Rotation")
    var rotation: String = "Default"

    @SerialName("CustomRotate")
    var customRotate: SerializedVec3 = Vec3.ZERO

    @SerialName("Part")
    var part: OBB.Part = OBB.Part.BODY

    @Transient
    @kotlinx.serialization.Transient
    private var obb: OBB? = null

    fun getOBB(): OBB {
        if (this.obb == null) {
            this.obb = OBB(
                OBB.vec3ToVector3d(Vec3.ZERO),
                OBB.vec3ToVector3d(this.size),
                Quaterniond(),
                this.part
            )
        }
        return this.obb!!
    }

    fun limit() {
        if (this.transform == null || this.transform!!.isBlank()) this.transform = "Vehicle"
    }
}
