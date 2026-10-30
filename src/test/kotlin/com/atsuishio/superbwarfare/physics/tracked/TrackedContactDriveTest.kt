package com.atsuishio.superbwarfare.physics.tracked

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

class TrackedContactDriveTest {
    @Test
    fun torqueCanLowerTheAuthoredFriction() {
        val mu = TrackedContactDrive.frictionCoefficient(
            authoredFriction = 0.8,
            torqueNm = 1_000.0,
            radiusM = 0.5,
            supportedMassKg = 10_000.0,
        )
        assertEquals(1_000.0 / 0.5 / (10_000.0 * 9.81), mu, 1e-9)
    }

    @Test
    fun aLargeTorqueDoesNotExceedAuthoredFriction() {
        val mu = TrackedContactDrive.frictionCoefficient(
            authoredFriction = 0.8,
            torqueNm = 160_000.0,
            radiusM = 0.3,
            supportedMassKg = 27_500.0,
        )
        assertEquals(0.8, mu, 1e-9)
    }

    @Test
    fun nonFiniteTorqueKeepsTheAuthoredCoefficient() {
        assertEquals(
            0.8,
            TrackedContactDrive.frictionCoefficient(0.8, Double.NaN, 0.3, 1_000.0),
            0.0,
        )
    }

    @Test
    fun rollingLossDecaysTheFreeRollTargetWithoutReversingIt() {
        val target = TrackedContactDrive.coastTarget(10.0, 0.015, 60.0)
        assertTrue(target < 10.0, "a positive rolling loss must decay the target")
        assertTrue(target > 0.0, "one substep may not reverse the belt")
        assertEquals(10.0 * kotlin.math.exp(-0.015 / 60.0), target, 1e-12)
    }

    @Test
    fun zeroRollingLossLeavesTheFreeRollTargetAlone() {
        assertEquals(10.0, TrackedContactDrive.coastTarget(10.0, 0.0, 60.0), 0.0)
        assertEquals(10.0, TrackedContactDrive.coastTarget(10.0, Double.NaN, 60.0), 0.0)
    }

    @Test
    fun rollingLossCannotExceedOneSubstepDecay() {
        val target = TrackedContactDrive.coastTarget(10.0, 1.0, 60.0)
        assertTrue(target in 0.0..10.0)
        assertTrue(target < 10.0)
    }

    @Test
    fun aNegativeFreeRollSpeedKeepsItsSign() {
        val target = TrackedContactDrive.coastTarget(-8.0, 0.015, 60.0)
        assertTrue(target < 0.0, "reverse coast must stay reverse")
        assertTrue(target > -8.0, "reverse coast must lose speed toward zero")
    }

    @Test
    fun aFlatContactKeepsTheForwardDirection() {
        val frame = TrackedContactDrive.contactFrame(
            normal = Vec3(0.0, 1.0, 0.0),
            preferred = Vec3(0.0, 0.0, -1.0),
        )
        assertNotNull(frame, "a floor contact has a legal frame")
        assertEquals(-1.0, frame!!.dir1.z, 1.0e-9)
        assertEquals(0.0, frame.dir1.y, 1.0e-9)
        assertTrue(abs(frame.dir1.dot(frame.dir2)) < 1.0e-9, "the rows must be perpendicular")
        assertTrue(abs(frame.dir1.dot(Vec3(0.0, 1.0, 0.0))) < 1.0e-9, "dir1 must stay in the plane")
        assertTrue(abs(frame.dir2.dot(Vec3(0.0, 1.0, 0.0))) < 1.0e-9, "dir2 must stay in the plane")
    }

    @Test
    fun aRiserWithAnAssistTangentGetsAValidFrame() {
        // A vertical face: the chassis forward is parallel to the normal, so a
        // naive projection is zero. The assist tangent is the uphill direction
        // and must produce a usable orthonormal frame.
        val normal = Vec3(0.0, 0.0, -1.0)
        assertNull(
            TrackedContactDrive.projectOntoPlane(normal, Vec3(0.0, 0.0, -1.0)),
            "driving straight at a face has no in-plane projection",
        )
        val frame = TrackedContactDrive.contactFrame(normal, Vec3(0.0, 1.0, 0.0))
        assertNotNull(frame, "the uphill tangent is a legal friction row")
        assertEquals(1.0, frame!!.dir1.y, 1.0e-9)
        assertTrue(abs(frame.dir1.dot(normal)) < 1.0e-9)
        assertTrue(abs(frame.dir1.dot(frame.dir2)) < 1.0e-9)
        assertTrue(frame.dir2.lengthSqr() > 0.9, "the second row must not collapse")
    }

    @Test
    fun aPreferredDirectionParallelToTheNormalIsRefused() {
        // This is the defect this API exists to prevent: writing a friction row
        // along the normal itself. No direction may be fabricated.
        assertNull(TrackedContactDrive.contactFrame(Vec3(0.0, 0.0, -1.0), Vec3(0.0, 0.0, -1.0)))
        assertNull(TrackedContactDrive.contactFrame(Vec3(0.0, 0.0, -1.0), Vec3(0.0, 0.0, 1.0)))
        assertNull(TrackedContactDrive.contactFrame(Vec3.ZERO, Vec3(1.0, 0.0, 0.0)))
        assertNull(TrackedContactDrive.contactFrame(Vec3(0.0, 1.0, 0.0), Vec3(Double.NaN, 0.0, 0.0)))
    }

    @Test
    fun aSlopeProducesTwoUnitRows() {
        // Rises toward -Z, so its normal tilts toward +Z and driving toward -Z
        // is uphill.
        val normal = Vec3(0.0, 0.8, 0.6).normalize()
        val frame = TrackedContactDrive.contactFrame(normal, Vec3(0.0, 0.0, -1.0))
        assertNotNull(frame)
        assertEquals(1.0, frame!!.dir1.length(), 1.0e-9)
        assertEquals(1.0, frame.dir2.length(), 1.0e-9)
        assertTrue(frame.dir1.y > 0.0, "an uphill slope projection must point up the slope")
    }
}
