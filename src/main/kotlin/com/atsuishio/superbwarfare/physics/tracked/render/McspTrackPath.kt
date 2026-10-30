package com.atsuishio.superbwarfare.physics.tracked.render

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Visible belt envelope shared with the physical wheel order.
 *
 * This is the MCSP `PhysLoop` construction: tangents, directed wrap arcs, and a
 * zero sweep when a wheel is collinear or lifted out of the envelope. It does
 * not read Bullet. A near-zero wrap must not become a full turn; that is what
 * made plates orbit the road wheels.
 *
 * Coordinates accepted and returned here are chassis Y/Z. Internally the loop
 * uses model Z (`-chassis Z`), which is the axis MCSP feeds to `PhysLoop`.
 */
internal object McspTrackPath {
    private const val TWO_PI = PI * 2.0
    private const val FULL_TURN_SLACK = 1.0e-6

    data class Wheel(
        val y: Double,
        val z: Double,
        val radius: Double,
        val outside: Boolean,
    )

    data class Sample(
        val y: Double,
        val z: Double,
        val tangentY: Double,
        val tangentZ: Double,
    )

    /**
     * Template binding from [PlatformGearDraw.build].
     *
     * [along] and [normal] are metres in the model loop. [angle] is the template's
     * rest tangent angle. [distance] is the rest arc length nearest the template pivot.
     */
    data class Seat(
        val along: Double,
        val normal: Double,
        val angle: Double,
        val distance: Double,
    )

    /** Chassis position and baker `rotationX` for one inserted plate. */
    data class LinkPose(
        val y: Double,
        val z: Double,
        val rotationX: Float,
    )

    class Path internal constructor(
        val length: Double,
        private val pieces: List<Piece>,
    ) {
        fun at(distance: Double): Sample {
            if (pieces.isEmpty() || length <= 1.0e-6) return Sample(0.0, 0.0, 0.0, 1.0)
            val value = wrap(distance, length)
            val piece = pieces[pieceIndex(value)]
            val u = value - piece.s0
            return if (piece.arc) {
                val angle = if (piece.c > 0.0) piece.d + piece.e * u / piece.c else piece.d
                val modelZ = piece.a + piece.c * cos(angle)
                val modelY = piece.b + piece.c * sin(angle)
                Sample(
                    y = modelY,
                    z = -modelZ,
                    tangentY = piece.e * cos(angle),
                    tangentZ = piece.e * sin(angle),
                )
            } else {
                Sample(
                    y = piece.b + piece.d * u,
                    z = -(piece.a + piece.c * u),
                    tangentY = piece.d,
                    tangentZ = -piece.c,
                )
            }
        }

        private fun pieceIndex(value: Double): Int {
            var low = 0
            var high = pieces.lastIndex
            while (low < high) {
                val mid = (low + high + 1) ushr 1
                if (pieces[mid].s0 <= value) low = mid else high = mid - 1
            }
            return low
        }
    }

    /**
     * Solve the closed envelope.
     *
     * [Wheel.radius] is the wheel collision radius. [thickness] is the belt
     * thickness. MCSP passes `contactRadius - thickness` and then adds half the
     * thickness inside the loop, so the centre line is `R + thickness / 2`.
     */
    fun solve(wheels: List<Wheel>, thickness: Double): Path? {
        if (wheels.size < 2 || thickness < 0.0 || !thickness.isFinite()) return null
        if (wheels.any { it.radius < 0.0 || !it.radius.isFinite() || !it.y.isFinite() || !it.z.isFinite() }) return null
        val count = wheels.size
        val radius = DoubleArray(count) { wheels[it].radius + 0.5 * thickness }
        if (radius.any { it <= 1.0e-6 }) return null
        val sigma = orientation(wheels)
        val side = DoubleArray(count) { index -> if (wheels[index].outside) sigma else -sigma }
        val theta = DoubleArray(count)
        val span = DoubleArray(count)
        for (index in wheels.indices) {
            if (!tangent(wheels, radius, side, index, (index + 1) % count, theta, span)) return null
        }
        val kinked = BooleanArray(count)
        val kinkY = DoubleArray(count)
        val kinkZ = DoubleArray(count)
        val cut = DoubleArray(count)
        if (count > 2) {
            for (index in wheels.indices) {
                val previous = (index + count - 1) % count
                val next = (index + 1) % count
                if (!lifted(wheels, radius, side, index, previous, next)) continue
                kink(wheels, radius, side, theta, index, previous, kinkY, kinkZ, cut)
                kinked[index] = true
            }
        }
        val pieces = ArrayList<Piece>(count * 2)
        var distance = 0.0
        for (index in wheels.indices) {
            val incoming = theta[(index + count - 1) % count]
            val outgoing = theta[index]
            val sign = side[index]
            var sweep = if (kinked[index]) 0.0 else normalize(sign * (outgoing - incoming))
            if (sweep > TWO_PI - FULL_TURN_SLACK) sweep = 0.0
            val start = incoming - sign * PI / 2.0
            val end = outgoing - sign * PI / 2.0
            val arcRadius = if (kinked[index]) 0.0 else radius[index]
            val centerZ = if (kinked[index]) kinkZ[index] else -wheels[index].z
            val centerY = if (kinked[index]) kinkY[index] else wheels[index].y
            pieces += Piece(true, distance, centerZ, centerY, arcRadius, start, sign)
            distance += arcRadius * sweep
            val run = max(0.0, span[index] - cut[index] - cut[(index + 1) % count])
            pieces += Piece(
                false,
                distance,
                centerZ + arcRadius * cos(end),
                centerY + arcRadius * sin(end),
                cos(outgoing),
                sin(outgoing),
                0.0,
            )
            distance += run
        }
        if (!distance.isFinite() || distance <= 1.0e-4) return null
        return Path(distance, pieces)
    }

    fun visibleCount(restLength: Double, visiblePitch: Double): Int {
        if (!restLength.isFinite() || restLength <= 0.0 || !visiblePitch.isFinite() || visiblePitch <= 1.0e-4) return 0
        return max(1, ceil(restLength / visiblePitch - 0.001).toInt())
    }

    /**
     * Bind the template pivot to the rest envelope.
     *
     * [pivotY]/[pivotZ] are chassis coordinates. [forwardY]/[forwardZ] are the
     * template's model-space +Z after its authored rotation.
     */
    fun calibrate(
        path: Path,
        pivotY: Double,
        pivotZ: Double,
        forwardY: Double,
        forwardZ: Double,
    ): Seat {
        val distance = nearest(path, pivotY, pivotZ)
        val sample = path.at(distance)
        val pointY = sample.y
        val pointZ = -sample.z
        val tangentY = sample.tangentY
        val tangentZ = -sample.tangentZ
        var alignedY = forwardY
        var alignedZ = forwardZ
        val length = hypot(alignedY, alignedZ)
        if (length > 1.0e-9) {
            alignedY /= length
            alignedZ /= length
        } else {
            alignedY = 0.0
            alignedZ = 1.0
        }
        if (alignedZ * tangentZ + alignedY * tangentY < 0.0) {
            alignedY = -alignedY
            alignedZ = -alignedZ
        }
        val deltaZ = -pivotZ - pointZ
        val deltaY = pivotY - pointY
        return Seat(
            along = deltaZ * alignedZ + deltaY * alignedY,
            normal = -deltaZ * alignedY + deltaY * alignedZ,
            angle = atan2(alignedY, alignedZ),
            distance = distance,
        )
    }

    /** MCSP `link`, converted back to chassis coordinates. */
    fun linkPose(sample: Sample, along: Double, normal: Double, angle: Double): LinkPose {
        val tangentY = sample.tangentY
        val tangentZ = -sample.tangentZ
        val y = sample.y + along * tangentY + normal * tangentZ
        val z = -sample.z + along * tangentZ - normal * tangentY
        return LinkPose(y, -z, (-(atan2(tangentY, tangentZ) - angle)).toFloat())
    }

    internal data class Piece(
        val arc: Boolean,
        val s0: Double,
        val a: Double,
        val b: Double,
        val c: Double,
        val d: Double,
        val e: Double,
    )

    private fun orientation(wheels: List<Wheel>): Double {
        var area = 0.0
        for (index in wheels.indices) {
            val current = wheels[index]
            val next = wheels[(index + 1) % wheels.size]
            val z = -current.z
            val nextZ = -next.z
            area += z * next.y - nextZ * current.y
        }
        return if (area < 0.0) -1.0 else 1.0
    }

    private fun tangent(
        wheels: List<Wheel>,
        radius: DoubleArray,
        side: DoubleArray,
        from: Int,
        to: Int,
        theta: DoubleArray,
        span: DoubleArray,
    ): Boolean {
        val dz = -wheels[to].z - (-wheels[from].z)
        val dy = wheels[to].y - wheels[from].y
        val length = hypot(dz, dy)
        val delta = side[to] * radius[to] - side[from] * radius[from]
        val tolerance = 1.0e-9 * max(1.0, length)
        if (length <= tolerance || abs(delta) > length + tolerance) return false
        theta[from] = atan2(dy, dz) - asin(clamp(delta / length))
        span[from] = sqrt(max(0.0, length * length - delta * delta))
        return true
    }

    private fun lifted(
        wheels: List<Wheel>,
        radius: DoubleArray,
        side: DoubleArray,
        index: Int,
        previous: Int,
        next: Int,
    ): Boolean {
        if (previous == next) return false
        val rho = side[previous] * radius[previous]
        val dz = -wheels[next].z - (-wheels[previous].z)
        val dy = wheels[next].y - wheels[previous].y
        val length = hypot(dz, dy)
        val delta = side[next] * radius[next] - rho
        val tolerance = 1.0e-9 * max(1.0, length)
        if (length <= tolerance || abs(delta) > length + tolerance) return false
        val angle = atan2(dy, dz) - asin(clamp(delta / length))
        val normalZ = -sin(angle)
        val normalY = cos(angle)
        val pointZ = -wheels[previous].z - rho * normalZ
        val pointY = wheels[previous].y - rho * normalY
        val offset = (-wheels[index].z - pointZ) * normalZ + (wheels[index].y - pointY) * normalY
        return side[index] * offset > radius[index] + 1.0e-9 * max(1.0, abs(offset))
    }

    private fun kink(
        wheels: List<Wheel>,
        radius: DoubleArray,
        side: DoubleArray,
        theta: DoubleArray,
        index: Int,
        previous: Int,
        kinkY: DoubleArray,
        kinkZ: DoubleArray,
        cut: DoubleArray,
    ) {
        val incoming = theta[previous]
        val turn = Math.IEEEremainder(theta[index] - incoming, TWO_PI)
        val angle = incoming + 0.5 * turn - side[index] * PI / 2.0
        val reach = radius[index] / cos(0.5 * turn)
        kinkZ[index] = -wheels[index].z + reach * cos(angle)
        kinkY[index] = wheels[index].y + reach * sin(angle)
        cut[index] = radius[index] * kotlin.math.tan(0.5 * abs(turn))
    }

    private fun nearest(path: Path, y: Double, z: Double): Double {
        val length = path.length
        var best = 0.0
        var bestDistance = Double.POSITIVE_INFINITY
        val samples = 2048
        for (index in 0 until samples) {
            val distance = length * index / samples
            val sample = path.at(distance)
            val error = (sample.y - y) * (sample.y - y) + (sample.z - z) * (sample.z - z)
            if (error < bestDistance) {
                bestDistance = error
                best = distance
            }
        }
        var low = best - length / samples
        var high = best + length / samples
        repeat(60) {
            val left = low + (high - low) / 3.0
            val right = high - (high - low) / 3.0
            val leftSample = path.at(left)
            val rightSample = path.at(right)
            val leftError = (leftSample.y - y) * (leftSample.y - y) + (leftSample.z - z) * (leftSample.z - z)
            val rightError = (rightSample.y - y) * (rightSample.y - y) + (rightSample.z - z) * (rightSample.z - z)
            if (leftError < rightError) high = right else low = left
        }
        return wrap(0.5 * (low + high), length)
    }

    private fun wrap(value: Double, cycle: Double): Double {
        if (cycle <= 0.0 || !value.isFinite()) return 0.0
        var result = value % cycle
        if (result < 0.0) result += cycle
        if (result >= cycle) result -= cycle
        return result
    }

    private fun normalize(angle: Double): Double {
        var result = angle % TWO_PI
        if (result < 0.0) result += TWO_PI
        return result
    }

    private fun clamp(value: Double): Double = value.coerceIn(-1.0, 1.0)

    private fun asin(value: Double): Double = kotlin.math.asin(value)
}
