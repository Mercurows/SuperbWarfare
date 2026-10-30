package com.atsuishio.superbwarfare.physics.tracked

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TrackedCollisionDebugTest {
    @Test
    fun anIdentityBoxHasTwelveEdgesAtItsExtents() {
        val out = ArrayList<Float>()
        TrackedCollisionDebug.appendBox(
            out,
            Vector3f(1f, 2f, 3f),
            Quaternion(),
            Vector3f(0.5f, 0.25f, 0.125f),
            TrackedCollisionDebug.KIND_HULL,
        )
        assertEquals(12, out.size / TrackedCollisionDebug.FLOATS_PER_SEGMENT)
        val xs = FloatArray(out.size / TrackedCollisionDebug.FLOATS_PER_SEGMENT * 2)
        var cursor = 0
        var slot = 0
        while (cursor + 8 <= out.size) {
            xs[slot++] = out[cursor]
            xs[slot++] = out[cursor + 3]
            assertEquals(TrackedCollisionDebug.KIND_HULL, out[cursor + 6])
            cursor += 8
        }
        assertTrue(xs.any { kotlin.math.abs(it - 0.5f) < 1.0e-4f })
        assertTrue(xs.any { kotlin.math.abs(it - 1.5f) < 1.0e-4f })
    }

    @Test
    fun aWheelCylinderUsesTheLocalXAxis() {
        val out = ArrayList<Float>()
        TrackedCollisionDebug.appendCylinder(
            out,
            Vector3f(0f, 0f, 0f),
            Quaternion(),
            0.2f,
            0.4f,
            TrackedCollisionDebug.KIND_WHEEL,
        )
        assertTrue(out.size / TrackedCollisionDebug.FLOATS_PER_SEGMENT >= 16)
        var sawWidth = false
        var cursor = 0
        while (cursor + 8 <= out.size) {
            val ax = out[cursor]
            val bx = out[cursor + 3]
            if (kotlin.math.abs(ax - bx) > 0.3f) sawWidth = true
            assertEquals(TrackedCollisionDebug.KIND_WHEEL, out[cursor + 6])
            cursor += 8
        }
        assertTrue(sawWidth, "the cylinder generators must span the wheel width")
    }
}
