package com.atsuishio.superbwarfare.data.vehicle.subdata

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import com.atsuishio.superbwarfare.tools.OBB
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond

/**
 * One oriented bounding box entry of a vehicle.
 *
 * Declared as a data class so `copy()` is generated: every persisted field is an immutable value
 * ([SerializedVec3], String or enum), and the transient lazily-built [OBB] lives in the class body, so
 * the generated `copy()` is exactly the per-entity shallow copy vehicles need (a fresh [OBB] per
 * instance, since vehicles mutate their own OBB instances).
 */
@Serializable
data class OBBInfo(
    @SerialName("Size")
    var size: SerializedVec3 = Vec3.ZERO,

    @SerialName("Position")
    var position: SerializedVec3 = Vec3.ZERO,

    @SerialName("Transform")
    var transform: String? = "Default",

    @SerialName("Rotation")
    var rotation: String = "Default",

    @SerialName("CustomRotate")
    var customRotate: SerializedVec3 = Vec3.ZERO,

    @SerialName("Part")
    var part: OBB.Part = OBB.Part.BODY,
) {
    /**
     * Lazily built collision box, captured from [size] / [part] on first access.
     *
     * Held as an explicit [Lazy] field rather than a `by lazy` delegate on purpose: a delegated
     * property cannot carry a field annotation, and its generated `obb$delegate` field would be picked
     * up by Gson (which backs `IDBasedData.copy()` and only skips `transient` fields).
     *
     * Kept in the class body so the generated `copy()` does not share it: each vehicle mutates its own
     * [OBB] instances.
     */
    @Transient
    @kotlinx.serialization.Transient
    private val obbLazy: Lazy<OBB> = lazy {
        OBB(
            OBB.vec3ToVector3d(Vec3.ZERO),
            OBB.vec3ToVector3d(this.size),
            Quaterniond(),
            this.part
        )
    }

    fun getOBB(): OBB = obbLazy.value

    fun limit() {
        if (this.transform == null || this.transform!!.isBlank()) this.transform = "Vehicle"
    }
}
