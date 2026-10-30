package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Static interference audit of the physical belt runs.
 *
 * The legacy shoe audit answers a different question: it measures a pin-chord
 * shoe against the wheels it wraps. A run is a box on the belt's centre line, so
 * the number that decides whether the mechanism can be assembled is the distance
 * between that box and each wheel's solid collider, not the distance from a
 * wheel centre to one face of the box.
 *
 * The audit reports every wheel that overlaps the run laterally, not just the
 * pair the run connects: a run that passes through an intermediate road wheel is
 * a modelling defect even though that wheel is not one of its two ends.
 *
 * Adjacent runs meet at a shared wheel and are expected to overlap in the wedge
 * between their two tangents. That overlap is excluded from the intersection
 * list; only non-adjacent runs folding back into each other are reported.
 */
object TrackedRunGeometryAudit {

    /**
     * Below this the pair counts as touching rather than clear.
     *
     * This is an assembly tolerance, not a vehicle acceptance limit: a run may
     * legitimately touch its own wheel, but overlapping collision shapes by more
     * than a millimetre would resolve as a jolt at assembly.
     */
    const val CONTACT_TOLERANCE_M = 0.001

    data class Result(
        /** Smallest gap between a run box and any wheel, metres. May be negative. */
        val minRunWheelGapM: Double,
        val worstRunIndex: Int,
        val worstWheelBone: String,
        /** Lateral overlap of the worst pair, metres. */
        val worstLateralOverlapM: Double,
        /** Number of run/wheel pairs that overlap laterally and were measured. */
        val checkedPairs: Int,
        /** Non-adjacent run pairs whose boxes intersect, as `(i, j)` with `i < j`. */
        val intersectingRunPairs: List<Pair<Int, Int>>,
    ) {
        val interpenetrates: Boolean get() = minRunWheelGapM < -CONTACT_TOLERANCE_M

        fun problems(): List<String> = buildList {
            if (interpenetrates) {
                add(
                    "run $worstRunIndex overlaps wheel $worstWheelBone by " +
                        "%.2f mm".format(-minRunWheelGapM * 1000.0),
                )
            }
            if (intersectingRunPairs.isNotEmpty()) {
                add("non-adjacent run boxes intersect: $intersectingRunPairs")
            }
        }
    }

    /** Measure every run against every wheel and against every non-adjacent run. */
    fun audit(
        runs: List<CompiledRun>,
        wheels: List<CompiledWheel>,
        belt: CompiledBeltGeometry,
    ): Result {
        require(runs.isNotEmpty()) { "no runs to audit" }

        var worst = Double.POSITIVE_INFINITY
        var worstRun = -1
        var worstWheel = ""
        var worstOverlap = 0.0
        var pairs = 0

        for (run in runs) {
            for (wheel in wheels) {
                val lateral = lateralOverlap(run, wheel, belt)
                if (lateral <= 0.0) continue
                pairs++
                val gap = boxWheelGap(run, wheel)
                if (gap < worst) {
                    worst = gap
                    worstRun = run.index
                    worstWheel = wheel.bone
                    worstOverlap = lateral
                }
            }
        }

        val intersections = ArrayList<Pair<Int, Int>>()
        for (i in runs.indices) {
            for (j in i + 1 until runs.size) {
                if (adjacent(runs, i, j)) continue
                if (boxesIntersect(runs[i], runs[j])) intersections += runs[i].index to runs[j].index
            }
        }

        return Result(
            minRunWheelGapM = worst,
            worstRunIndex = worstRun,
            worstWheelBone = worstWheel,
            worstLateralOverlapM = worstOverlap,
            checkedPairs = pairs,
            intersectingRunPairs = intersections,
        )
    }

    /**
     * Lateral overlap between a run box and a wheel's cylinder, metres.
     *
     * The run is centred on the belt plane, so this is a pure interval overlap on
     * the lateral axis. A pair with no lateral overlap cannot collide whatever
     * the in-plane distance is.
     */
    fun lateralOverlap(run: CompiledRun, wheel: CompiledWheel, belt: CompiledBeltGeometry): Double {
        val runHalf = run.halfExtents.x
        val runCenter = belt.centerPlaneX
        val wheelHalf = wheel.halfWidth
        val wheelCenter = wheel.assemblyCenter.x
        return min(runCenter + runHalf, wheelCenter + wheelHalf) -
            max(runCenter - runHalf, wheelCenter - wheelHalf)
    }

    /**
     * Distance between the run box and the wheel's circular section, metres.
     *
     * Negative when they overlap. The wheel is projected into the run's own frame,
     * where the box is axis aligned and the wheel section is a circle because its
     * axle is lateral.
     */
    fun boxWheelGap(run: CompiledRun, wheel: CompiledWheel): Double {
        val local = TrackedRunGeometry.inverseRotateAboutX(
            wheel.assemblyCenter.subtract(run.center),
            run.rotation,
        )
        val closestY = local.y.coerceIn(-run.halfExtents.y, run.halfExtents.y)
        val closestZ = local.z.coerceIn(-run.halfExtents.z, run.halfExtents.z)
        return hypot(local.y - closestY, local.z - closestZ) - wheel.radius
    }

    /**
     * Whether two run boxes overlap.
     *
     * Runs only rotate about `X`, so each box projects to a true rectangle in the
     * `(y, z)` plane. This is an exact separating-axis test on those two
     * rectangles plus a lateral interval overlap.
     */
    fun boxesIntersect(first: CompiledRun, second: CompiledRun): Boolean {
        val delta = second.center.subtract(first.center)
        if (abs(delta.x) >= first.halfExtents.x + second.halfExtents.x - CONTACT_TOLERANCE_M) return false

        val firstAngle = angleOf(first)
        val secondAngle = angleOf(second)
        val firstAxes = localAxes(firstAngle)
        val secondAxes = localAxes(secondAngle)
        val firstHalf = doubleArrayOf(first.halfExtents.y, first.halfExtents.z)
        val secondHalf = doubleArrayOf(second.halfExtents.y, second.halfExtents.z)

        for (axis in firstAxes + secondAxes) {
            val distance = abs(delta.y * axis[0] + delta.z * axis[1])
            val firstRadius = supportRadius(axis, firstAxes, firstHalf)
            val secondRadius = supportRadius(axis, secondAxes, secondHalf)
            if (distance >= firstRadius + secondRadius - CONTACT_TOLERANCE_M) return false
        }
        return true
    }

    /** True when two runs share a wheel, so they are expected to meet in a wedge. */
    private fun adjacent(runs: List<CompiledRun>, first: Int, second: Int): Boolean {
        val a = runs[first]
        val b = runs[second]
        return a.firstWheelIndex == b.firstWheelIndex ||
            a.firstWheelIndex == b.secondWheelIndex ||
            a.secondWheelIndex == b.firstWheelIndex ||
            a.secondWheelIndex == b.secondWheelIndex
    }

    private fun angleOf(run: CompiledRun): Double =
        2.0 * atan2(run.rotation.x.toDouble(), run.rotation.w.toDouble())

    /**
     * Local `(y, z)` axis directions of a box rotated about `X`.
     *
     * The rotation maps local `+Y` to `(cos, sin)` and local `+Z` to `(-sin, cos)`
     * in the `(y, z)` plane.
     */
    private fun localAxes(angle: Double): List<DoubleArray> = listOf(
        doubleArrayOf(cos(angle), sin(angle)),
        doubleArrayOf(-sin(angle), cos(angle)),
    )

    /** Support radius of a rectangle with [half] extents along [axes] onto [axis]. */
    private fun supportRadius(axis: DoubleArray, axes: List<DoubleArray>, half: DoubleArray): Double =
        abs(axis[0] * axes[0][0] + axis[1] * axes[0][1]) * half[0] +
            abs(axis[0] * axes[1][0] + axis[1] * axes[1][1]) * half[1]
}
