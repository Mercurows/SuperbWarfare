package com.atsuishio.superbwarfare.physics.tracked

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * World-space edges of the collision shapes a tracked vehicle actually has.
 *
 * This is display data. It is built from the physics-thread pose and the
 * compiled shape, then copied out as plain floats. A renderer must not turn
 * it back into a body, and reading it must not wake or move anything.
 */
object TrackedCollisionDebug {
    const val FLOATS_PER_SEGMENT = 8
    const val CHUNK_SEGMENTS = 800
    const val MAX_SEGMENTS = 4_096
    const val KIND_HULL = 1f
    const val KIND_WHEEL = 2f
    const val KIND_RUN = 3f

    class Snapshot(
        val epoch: Long,
        val segments: FloatArray,
        val truncated: Boolean,
    ) {
        val segmentCount: Int get() = segments.size / FLOATS_PER_SEGMENT
    }

    /** Twelve edges of one oriented box. [half] is the local half extents. */
    fun appendBox(
        out: ArrayList<Float>,
        center: Vector3f,
        rotation: Quaternion,
        half: Vector3f,
        kind: Float,
    ) {
        if (out.size / FLOATS_PER_SEGMENT >= MAX_SEGMENTS) return
        val corners = Array(8) { index ->
            val x = if (index and 1 == 0) -half.x else half.x
            val y = if (index and 2 == 0) -half.y else half.y
            val z = if (index and 4 == 0) -half.z else half.z
            world(center, rotation, x, y, z)
        }
        val edges = intArrayOf(
            0, 1, 2, 3, 4, 5, 6, 7,
            0, 2, 1, 3, 4, 6, 5, 7,
            0, 4, 1, 5, 2, 6, 3, 7,
        )
        var cursor = 0
        while (cursor + 1 < edges.size) {
            append(out, corners[edges[cursor]], corners[edges[cursor + 1]], kind)
            cursor += 2
        }
    }

    /**
     * Wireframe of a cylinder whose axis is local X.
     *
     * That is the wheel shape installed in Bullet: half-width along X, radius
     * in Y and Z. Eight segments are enough to see the cylinder without
     * pretending the display is the contact manifold.
     */
    fun appendCylinder(
        out: ArrayList<Float>,
        center: Vector3f,
        rotation: Quaternion,
        halfWidth: Float,
        radius: Float,
        kind: Float,
    ) {
        if (out.size / FLOATS_PER_SEGMENT >= MAX_SEGMENTS) return
        val segments = 8
        val left = Array(segments) { index ->
            val angle = index * Math.PI.toFloat() * 2f / segments
            world(center, rotation, -halfWidth, cos(angle) * radius, sin(angle) * radius)
        }
        val right = Array(segments) { index ->
            val angle = index * Math.PI.toFloat() * 2f / segments
            world(center, rotation, halfWidth, cos(angle) * radius, sin(angle) * radius)
        }
        for (index in 0 until segments) {
            val next = (index + 1) % segments
            append(out, left[index], left[next], kind)
            append(out, right[index], right[next], kind)
        }
        for (index in 0 until segments step 2) {
            append(out, left[index], right[index], kind)
        }
    }

    fun worldHalf(rotation: Quaternion, half: Vector3f): Vector3f {
        val matrix = rotation.toRotationMatrix()
        val ax = matrix.mult(Vector3f(1f, 0f, 0f), Vector3f())
        val ay = matrix.mult(Vector3f(0f, 1f, 0f), Vector3f())
        val az = matrix.mult(Vector3f(0f, 0f, 1f), Vector3f())
        return Vector3f(
            abs(ax.x) * half.x + abs(ay.x) * half.y + abs(az.x) * half.z,
            abs(ax.y) * half.x + abs(ay.y) * half.y + abs(az.y) * half.z,
            abs(ax.z) * half.x + abs(ay.z) * half.y + abs(az.z) * half.z,
        )
    }

    private fun world(center: Vector3f, rotation: Quaternion, x: Float, y: Float, z: Float): Vector3f =
        rotation.toRotationMatrix().mult(Vector3f(x, y, z), Vector3f()).addLocal(center)

    private fun append(out: ArrayList<Float>, a: Vector3f, b: Vector3f, kind: Float) {
        if (out.size / FLOATS_PER_SEGMENT >= MAX_SEGMENTS) return
        if (!a.x.isFinite() || !a.y.isFinite() || !a.z.isFinite()) return
        if (!b.x.isFinite() || !b.y.isFinite() || !b.z.isFinite()) return
        out += a.x
        out += a.y
        out += a.z
        out += b.x
        out += b.y
        out += b.z
        out += kind
        out += 0f
    }
}
