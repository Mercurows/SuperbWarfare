package com.atsuishio.superbwarfare.physics.tracked.render

import kotlin.math.hypot

/**
 * Belt centre-line plate under a wheel.
 *
 * This is a geometric baseline shared by the visible sampler. It is not a Bullet
 * body and it does not move the idler. The centre sits one half-thickness
 * outside the authored wheel radius, which is the same surface the run boxes use.
 */
internal object BeltContactBaseline {
    data class Plate(
        val y: Double,
        val z: Double,
        val distance: Double,
    )

    /**
     * How far the rendered plate centre is from the belt centre line, metres.
     *
     * Positive means the plate is inside the centre line and must move outward.
     * The sign of [seatNormal] is resolved by the caller because path-frame
     * normal direction follows the tangent.
     */
    fun radialError(
        wheelY: Double,
        wheelZ: Double,
        radius: Double,
        thickness: Double,
        poseY: Double,
        poseZ: Double,
    ): Double? {
        if (radius <= 0.0 || thickness < 0.0) return null
        val centre = radius + thickness * 0.5
        val actual = hypot(poseY - wheelY, poseZ - wheelZ)
        if (!actual.isFinite() || !centre.isFinite()) return null
        return centre - actual
    }

    fun nearest(path: McspTrackPath.Path, y: Double, z: Double): Plate? {
        if (path.length <= 1.0e-6) return null
        val steps = 256
        var best = 0.0
        var bestDistance = Double.POSITIVE_INFINITY
        for (step in 0 until steps) {
            val distance = path.length * step / steps
            val sample = path.at(distance)
            val gap = hypot(sample.y - y, sample.z - z)
            if (gap < bestDistance) {
                bestDistance = gap
                best = distance
            }
        }
        var cursor = best
        var window = path.length / steps
        repeat(8) {
            val left = cursor - window
            val right = cursor + window
            val leftGap = gap(path, left, y, z)
            val rightGap = gap(path, right, y, z)
            val midGap = gap(path, cursor, y, z)
            cursor = when {
                leftGap <= rightGap && leftGap <= midGap -> left
                rightGap <= leftGap && rightGap <= midGap -> right
                else -> cursor
            }
            window *= 0.5
        }
        val sample = path.at(cursor)
        return Plate(sample.y, sample.z, cursor)
    }

    private fun gap(path: McspTrackPath.Path, distance: Double, y: Double, z: Double): Double {
        val sample = path.at(distance)
        return hypot(sample.y - y, sample.z - z)
    }
}
