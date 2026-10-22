package com.atsuishio.superbwarfare.physics.tracked

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Static interference audit for one assembled belt.
 *
 * S1 used to report the pin-polygon clearance as if it were a shoe clearance.
 * On the 99A that number was `+31.49 mm` while the physical shoe overlapped the
 * idler by `17.34 mm`, because a chord spanning a wrapped wheel sags toward the
 * wheel by its sagitta. The two numbers answer different questions and both are
 * reported, but only [CompiledGeometryAudit.minShoeWheelGapM] may gate assembly.
 */
object TrackedGeometryAudit {

    /**
     * Measure every shoe/wheel pair of one side.
     *
     * [wheels] must carry the *assembly* centres, so the idler already includes
     * the solved tensioner offset. [pinChordClearanceM] is carried through from
     * the solver so one result object reports both metrics and a caller cannot
     * quote one while believing it is the other.
     */
    fun audit(
        placements: List<CompiledLinkPlacement>,
        wheels: List<CompiledWheel>,
        shoeHalfWidth: Double,
        pinChordClearanceM: Double,
    ): CompiledGeometryAudit {
        require(placements.isNotEmpty()) { "no link placements" }

        var worst = Double.POSITIVE_INFINITY
        var worstLink = -1
        var worstWheel: String? = null
        var worstOverlap = 0.0
        var pairs = 0

        for (placement in placements) {
            for (wheel in wheels) {
                val gap = TrackedAssemblyGeometry.shoeWheelGap(
                    placement = placement,
                    shoeHalfWidth = shoeHalfWidth,
                    wheelCenter = wheel.assemblyCenter,
                    wheelRadius = wheel.radius,
                    wheelHalfWidth = wheel.halfWidth,
                )
                if (gap == Double.POSITIVE_INFINITY) continue
                pairs++
                if (gap < worst) {
                    worst = gap
                    worstLink = placement.index
                    worstWheel = wheel.bone
                    worstOverlap = min(
                        placement.center.x + shoeHalfWidth,
                        wheel.assemblyCenter.x + wheel.halfWidth,
                    ) - max(
                        placement.center.x - shoeHalfWidth,
                        wheel.assemblyCenter.x - wheel.halfWidth,
                    )
                }
            }
        }

        return CompiledGeometryAudit(
            minPinChordClearanceM = pinChordClearanceM,
            minShoeWheelGapM = worst,
            worstLinkIndex = worstLink,
            worstWheelBone = worstWheel ?: "",
            worstLateralOverlapM = worstOverlap,
            checkedPairs = pairs,
        )
    }

    /** True when [angle] is within [limit] of zero, allowing one ulp of slack. */
    fun withinLimit(angleRad: Double, limitRad: Double): Boolean = abs(angleRad) <= limitRad + 1.0e-9

    /**
     * Largest absolute joint angle of an assembled belt, in degrees.
     *
     * This is measured on the same placements the adapter uses, so it cannot
     * disagree with the hinge limits handed to Bullet.
     */
    fun maxJointAngleDegrees(placements: List<CompiledLinkPlacement>): Double {
        if (placements.size < 3) return 0.0
        var worst = 0.0
        for (index in placements.indices) {
            val previous = placements[(index - 1 + placements.size) % placements.size]
            val next = placements[index]
            worst = max(worst, abs(Math.toDegrees(TrackedAssemblyGeometry.jointAngle(previous, next))))
        }
        return worst
    }
}
