package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3

/**
 * One member of the belt, in traversal order.
 *
 * The belt is a cyclic sequence, and the sequence itself is the physical
 * topology: every adjacent pair, including the last back to the first, owns one
 * run. [ordinal] is therefore not the same number as the wheel's table index,
 * and the two are never used interchangeably.
 */
data class CompiledBeltMember(
    /** Wheel table index this member resolves to. */
    val wheelIndex: Int,
    /** Position in the traversal order, 0-based. */
    val ordinal: Int,
    val bone: String,
    val wrap: TrackWrap,
)

/**
 * The belt's own geometry, resolved once per side.
 *
 * [centerPlaneX] is the audited lateral centre of the belt, not the mean of the
 * wheel centres: on 99A the two differ by more than 160 mm, and averaging the
 * wheels would shift the whole physical run sideways. Wheels keep their own
 * authored lateral positions; the difference appears in each run's anchors.
 */
data class CompiledBeltGeometry(
    val centerPlaneX: Double,
    /** Unit lateral direction of the belt plane; `+X` for the current planar model. */
    val lateralAxis: Vec3,
    val widthM: Double,
    val thicknessM: Double,
    /** Where [widthM] and [thicknessM] came from, for the audit trail. */
    val sizeSource: String,
) {
    init {
        require(widthM > 0.0 && widthM.isFinite()) { "belt width must be positive, was $widthM" }
        require(thicknessM > 0.0 && thicknessM.isFinite()) { "belt thickness must be positive, was $thicknessM" }
        require(centerPlaneX.isFinite()) { "belt centre plane must be finite, was $centerPlaneX" }
        require(lateralAxis.isFiniteVector() && lateralAxis.length() > 1.0e-9) {
            "belt lateral axis must be finite and non-zero, was $lateralAxis"
        }
    }

    /** Half the belt thickness; the run's normal half extent. */
    val halfThicknessM: Double get() = thicknessM * 0.5

    /**
     * Distance from a wheel's collision centre to the run's centre line.
     *
     * **SBW candidate mapping:** the run's inner face touches the wheel collider
     * at `R` and the belt is one thickness thick, so the centre line sits at
     * `R + t/2`. This is not a recovered MCSP native formula; it is valid only
     * for the authored radius = wheel collision radius and `t` = outer belt
     * thickness, and R0 must calibrate those sources before it is audited. See
     * [TrackedRunGeometry] for why the opposite sign is wrong.
     */
    fun runCenterOffset(wheelRadiusM: Double): Double {
        require(wheelRadiusM > 0.0 && wheelRadiusM.isFinite()) {
            "wheel radius must be positive, was $wheelRadiusM"
        }
        return wheelRadiusM + halfThicknessM
    }
}

internal fun Vec3.isFiniteVector(): Boolean = x.isFinite() && y.isFinite() && z.isFinite()
