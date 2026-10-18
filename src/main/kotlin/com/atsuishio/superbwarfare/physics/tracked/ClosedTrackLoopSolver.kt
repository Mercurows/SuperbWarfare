package com.atsuishio.superbwarfare.physics.tracked

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Static closed-loop construction for one belt.
 *
 * The belt is a fixed-pitch polygon: every link is a straight chord of exactly
 * [pitch] metres between two pin axes, and the pins wrap the outside of the
 * wheels. This is *assembly geometry only*. Nothing here is used at runtime to
 * place links: the runtime bodies are solved by Bullet. The solver exists so a
 * candidate link count, pitch and tensioner position can be audited before a
 * vehicle is built, and so the initial pose starts from a closed, non-jammed
 * loop instead of a straight row that the solver has to fold itself.
 *
 * The single free parameter is the idler's longitudinal offset. For a given
 * link count the solver bisects that offset until the polygon closes; a count
 * that cannot close inside the allowed tensioner travel is a hard failure, not
 * something to paper over by stretching the shoes.
 */
object ClosedTrackLoopSolver {

    /** A wheel envelope circle in the side's local (z, y) plane. */
    data class Circle(
        val z: Double,
        val y: Double,
        /** Wheel radius plus pin inset; the pin locus rides this circle. */
        val pinRadius: Double,
        /** Wheel radius alone, used for interference checks. */
        val wheelRadius: Double,
    )

    data class Pin(val z: Double, val y: Double)

    data class Result(
        val linkCount: Int,
        val pitch: Double,
        val totalLength: Double,
        /** Signed idler displacement along +Z that closes the loop. */
        val idlerOffset: Double,
        val closureError: Double,
        val maxPitchError: Double,
        val maxJointAngleDegrees: Double,
        val minWheelClearance: Double,
        val pins: List<Pin>,
    ) {
        val valid: Boolean
            get() = closureError.isFinite() && maxPitchError.isFinite() &&
                abs(closureError) <= MAX_CLOSURE_ERROR_M &&
                maxPitchError <= MAX_PITCH_ERROR_M
    }

    const val MAX_CLOSURE_ERROR_M = 1.0e-4
    const val MAX_PITCH_ERROR_M = 1.0e-5
    private const val SAMPLES = 2048

    /**
     * Solves one side. Returns `null` when the required idler offset falls
     * outside `idlerTravel`, which means the declared count and pitch cannot be
     * assembled with this running gear.
     */
    fun solve(
        circles: List<Circle>,
        pitch: Double,
        linkCount: Int,
        idlerIndex: Int,
        idlerTravel: Double,
    ): Result? {
        require(circles.size >= 2) { "a belt needs at least two wheels" }
        require(pitch > 1.0e-4) { "pitch must be positive" }
        require(idlerIndex in circles.indices) { "idler index out of range" }

        fun withOffset(offset: Double): List<Circle> = circles.mapIndexed { index, circle ->
            if (index == idlerIndex) circle.copy(z = circle.z + offset) else circle
        }

        fun residual(offset: Double): Double = walk(withOffset(offset), pitch, linkCount, traceOnly = true)?.first ?: Double.NaN

        var low = -idlerTravel
        var high = idlerTravel
        val lowResidual = residual(low)
        val highResidual = residual(high)
        if (!lowResidual.isFinite() || !highResidual.isFinite()) return null
        if (lowResidual * highResidual > 0.0) return null

        repeat(60) {
            val mid = (low + high) * 0.5
            val value = residual(mid)
            if (!value.isFinite()) return null
            if (value * lowResidual > 0.0) {
                low = mid
            } else {
                high = mid
            }
        }
        val offset = (low + high) * 0.5
        val walked = walk(withOffset(offset), pitch, linkCount, traceOnly = false) ?: return null
        val pins = walked.second ?: return null
        val joint = maxJointAngle(pins)
        return Result(
            linkCount = linkCount,
            pitch = pitch,
            totalLength = linkCount * pitch,
            idlerOffset = offset,
            closureError = walked.first,
            maxPitchError = maxPitchError(pins, pitch),
            maxJointAngleDegrees = joint,
            minWheelClearance = minClearance(pins, withOffset(offset)),
            pins = pins,
        )
    }

    /** Candidate link counts around the continuous envelope perimeter. */
    fun candidateCounts(circles: List<Circle>, pitch: Double): IntRange {
        val perimeter = envelopePerimeter(circles)
        if (!perimeter.isFinite() || perimeter <= 0.0) return 0..0
        val estimate = ceil(perimeter / pitch).toInt()
        return max(3, estimate - 2)..(estimate + 2)
    }

    fun envelopePerimeter(circles: List<Circle>): Double = Envelope.build(circles).length

    /**
     * Walks [count] equal chords of length [pitch] along the envelope.
     * Returns the closure residual and, when requested, the pin positions.
     */
    private fun walk(
        circles: List<Circle>,
        pitch: Double,
        count: Int,
        traceOnly: Boolean,
    ): Pair<Double, List<Pin>?>? {
        val envelope = Envelope.build(circles)
        if (!envelope.length.isFinite() || envelope.length <= 0.0) return null
        var distance = 0.0
        val pins = if (traceOnly) null else ArrayList<Pin>(count + 1)
        pins?.add(envelope.at(0.0))
        repeat(count) {
            val start = envelope.at(distance)
            // The chord length is monotone in the advance, so a bracket of one
            // pitch is always sufficient: a straight run gives exactly pitch and
            // a curved run needs slightly more.
            var lo = distance + pitch
            var hi = distance + pitch * 1.5
            var guard = 0
            while (hypot(
                    envelope.at(hi).z - start.z,
                    envelope.at(hi).y - start.y,
                ) < pitch
            ) {
                hi += pitch * 0.5
                if (guard++ > 64) return null
            }
            repeat(45) {
                val mid = (lo + hi) * 0.5
                val point = envelope.at(mid)
                if (hypot(point.z - start.z, point.y - start.y) < pitch) lo = mid else hi = mid
            }
            distance = (lo + hi) * 0.5
            pins?.add(envelope.at(distance))
        }
        val residual = distance - envelope.length
        return residual to pins
    }

    private fun maxPitchError(pins: List<Pin>, pitch: Double): Double {
        if (pins.size < 2) return Double.NaN
        var worst = 0.0
        for (index in 0 until pins.size - 1) {
            val a = pins[index]
            val b = pins[index + 1]
            worst = max(worst, abs(hypot(b.z - a.z, b.y - a.y) - pitch))
        }
        return worst
    }

    private fun maxJointAngle(pins: List<Pin>): Double {
        if (pins.size < 3) return 0.0
        var worst = 0.0
        for (index in 0 until pins.size - 1) {
            val previous = pins[if (index == 0) pins.size - 2 else index - 1]
            val current = pins[index]
            val next = pins[index + 1]
            val a = atan2(current.y - previous.y, current.z - previous.z)
            val b = atan2(next.y - current.y, next.z - current.z)
            val delta = abs(normalizeRadians(b - a))
            worst = max(worst, delta)
        }
        return Math.toDegrees(worst)
    }

    /** Smallest signed gap between a chord and any wheel surface (negative = overlap). */
    private fun minClearance(pins: List<Pin>, circles: List<Circle>): Double {
        var best = Double.POSITIVE_INFINITY
        for (index in 0 until pins.size - 1) {
            val a = pins[index]
            val b = pins[index + 1]
            val dz = b.z - a.z
            val dy = b.y - a.y
            val lengthSquared = dz * dz + dy * dy
            if (lengthSquared < 1.0e-12) continue
            for (circle in circles) {
                val t = (((circle.z - a.z) * dz + (circle.y - a.y) * dy) / lengthSquared).coerceIn(0.0, 1.0)
                val distance = hypot(a.z + dz * t - circle.z, a.y + dy * t - circle.y)
                best = min(best, distance - circle.wheelRadius)
            }
        }
        return best
    }

    private fun normalizeRadians(value: Double): Double {
        var result = value
        while (result > Math.PI) result -= Math.PI * 2.0
        while (result < -Math.PI) result += Math.PI * 2.0
        return result
    }

    /**
     * Convex envelope of a set of circles, as a closed polyline sampled on each
     * circle. Sampling keeps the construction obviously correct (the hull of a
     * point cloud cannot miss a tangent), and at 512 samples per wheel the
     * chord error is far below any mechanical tolerance.
     */
    private class Envelope private constructor(
        private val points: List<Pin>,
        private val cumulative: DoubleArray,
        val length: Double,
    ) {
        fun at(distance: Double): Pin {
            if (points.isEmpty()) return Pin(0.0, 0.0)
            var value = distance % length
            if (value < 0.0) value += length
            var index = 0
            while (index < points.size - 1 && value > cumulative[index + 1]) index++
            val a = points[index]
            val b = points[(index + 1) % points.size]
            val span = cumulative[index + 1] - cumulative[index]
            val t = if (span <= 1.0e-12) 0.0 else (value - cumulative[index]) / span
            return Pin(a.z + (b.z - a.z) * t, a.y + (b.y - a.y) * t)
        }

        companion object {
            private const val SAMPLES_PER_CIRCLE = 512

            fun build(circles: List<Circle>): Envelope {
                require(circles.isNotEmpty()) { "no circles" }
                val cloud = ArrayList<Pin>(circles.size * SAMPLES_PER_CIRCLE)
                for (circle in circles) {
                    for (index in 0 until SAMPLES_PER_CIRCLE) {
                        val angle = Math.PI * 2.0 * index / SAMPLES_PER_CIRCLE
                        cloud += Pin(
                            circle.z + circle.pinRadius * cos(angle),
                            circle.y + circle.pinRadius * sin(angle),
                        )
                    }
                }
                val hull = convexHull(cloud)
                if (hull.size < 3) throw TrackedGeometryException("degenerate circle envelope")
                // Close the ring explicitly so cumulative lengths cover one loop.
                val ring = hull + hull.first()
                val cumulative = DoubleArray(ring.size)
                for (index in 1 until ring.size) {
                    cumulative[index] = cumulative[index - 1] + hypot(
                        ring[index].z - ring[index - 1].z,
                        ring[index].y - ring[index - 1].y,
                    )
                }
                return Envelope(ring, cumulative, cumulative.last())
            }

            /** Andrew's monotone chain; deterministic and dependency-free. */
            private fun convexHull(points: List<Pin>): List<Pin> {
                val sorted = points.sortedWith(compareBy<Pin> { it.z }.thenBy { it.y })
                if (sorted.size < 3) return sorted
                val lower = ArrayList<Pin>(sorted.size)
                for (point in sorted) {
                    while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], point) <= 0.0) {
                        lower.removeAt(lower.size - 1)
                    }
                    lower += point
                }
                val upper = ArrayList<Pin>(sorted.size)
                for (point in sorted.asReversed()) {
                    while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], point) <= 0.0) {
                        upper.removeAt(upper.size - 1)
                    }
                    upper += point
                }
                lower.removeAt(lower.size - 1)
                upper.removeAt(upper.size - 1)
                return lower + upper
            }

            private fun cross(origin: Pin, a: Pin, b: Pin): Double =
                (a.z - origin.z) * (b.y - origin.y) - (a.y - origin.y) * (b.z - origin.z)
        }
    }
}
