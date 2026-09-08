package com.atsuishio.superbwarfare.resource.gun.pojo

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.joml.Vector3f

@Serializable
class ShootRecoilInfo {
    @JvmField
    @SerialName("Offset")
    var offset: SerializedVector3f = Vector3f(1f, 1f, 1f)

    @JvmField
    @SerialName("Rotation")
    var rotation: SerializedVector3f = Vector3f(1f, 1f, 1f)

    @JvmField
    @SerialName("ZoomRate")
    var zoomRate: Float = 0.5f

    @JvmField
    @SerialName("Speed")
    var speed: Float = 1f
}