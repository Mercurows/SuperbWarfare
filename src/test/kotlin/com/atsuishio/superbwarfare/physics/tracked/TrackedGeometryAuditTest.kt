package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Static interference checks for a chord-shaped shoe.
 *
 * The important case is the one S1 originally missed: a pin polygon that looks
 * clear of a wheel while the shoe box, which has thickness, is already inside
 * it. These tests pin the *rule* rather than the 99A's current numbers.
 */
class TrackedGeometryAuditTest {

    private val pitch = 0.23307
    private val pinInset = 0.048828125
    private val thickness = 1.5625 / 16.0
    private val length = 3.72912 / 16.0
    private val shoeHalfWidth = 0.390625

    /** Radius of a regular [count]-gon whose chord is exactly [pitch]. */
    private fun ringRadius(count: Int): Double = pitch / (2.0 * sin(PI / count))

    /** A closed belt wrapped on one circle: every chord is exactly one pitch. */
    private fun ringPins(count: Int): List<ClosedTrackLoopSolver.Pin> {
        val radius = ringRadius(count)
        val step = 2.0 * PI / count
        return (0 until count).map { index ->
            val angle = index * step
            ClosedTrackLoopSolver.Pin(radius * cos(angle), radius * sin(angle))
        }
    }

    private fun placements(pins: List<ClosedTrackLoopSolver.Pin>, boxCenter: Vec3 = Vec3.ZERO) =
        TrackedAssemblyGeometry.place(
            pins = pins,
            centerX = 0.0,
            boxCenterInPinFrame = boxCenter,
            halfExtents = Vec3(shoeHalfWidth, thickness / 2.0, length / 2.0),
        )

    private fun shoeCentreOffset(): Vec3 = Vec3(0.0, thickness / 2.0 - pinInset, 0.0)

    private fun wheel(bone: String, center: Vec3, radius: Double, halfWidth: Double = 0.2) =
        CompiledWheel(
            side = TrackedSide.LEFT,
            role = TrackWheelRole.SUPPORT,
            bone = bone,
            index = 0,
            bindCenter = center,
            assemblyCenter = center,
            radius = radius,
            halfWidth = halfWidth,
            axleDirection = Vec3(1.0, 0.0, 0.0),
            massKg = 900.0,
            suspension = CompiledSuspension(
                travelUp = 0.18, travelDown = 0.24, frequency = 2.2,
                stiffnessNPerM = 1.0, compressionDampingNsPerM = 1.0, reboundDampingNsPerM = 1.0,
                maxForceN = 400_000.0, bumpStopStart = 0.85, bumpStopExponent = 2.0,
                direction = Vec3(0.0, 1.0, 0.0), stationMassKg = 1000.0, staticCompressionM = 0.05,
            ),
        )

    @Test
    fun thePinLocusFormulaClearsTheShoeThickness() {
        val wheelRadius = 0.53125
        val clearance = 0.002
        val radius = ClosedTrackLoopSolver.pinLocusRadius(wheelRadius, pinInset, clearance, pitch)

        // Chord midpoint distance is exactly the shoe's inner face plus clearance.
        val midpoint = sqrt(radius * radius - (pitch / 2.0) * (pitch / 2.0))
        assertEquals(wheelRadius + pinInset + clearance, midpoint, 1e-12)

        // A naive locus on the wheel surface would sag by the sagitta.
        val naive = wheelRadius + pinInset
        val sagitta = naive - sqrt(naive * naive - (pitch / 2.0) * (pitch / 2.0))
        assertTrue(sagitta > 0.01, "the 99A guide wheels sag $sagitta m, which is a real defect")
    }

    /**
     * Regression for the S1 defect: a pin locus on the wheel surface plus the
     * inset leaves every wrapped chord tangent in the *pin* metric, yet the shoe
     * box is already inside the wheel by the chord sagitta.
     */
    @Test
    fun aChordClearanceCanHideAShoeInterpenetration() {
        val count = 64
        val radius = ringRadius(count)
        val pins = ringPins(count)
        val wheelRadius = radius - pinInset
        val sagitta = radius - sqrt(radius * radius - (pitch / 2.0) * (pitch / 2.0))

        val audit = TrackedGeometryAudit.audit(
            placements = placements(pins, shoeCentreOffset()),
            wheels = listOf(wheel("idler", Vec3.ZERO, wheelRadius)),
            shoeHalfWidth = shoeHalfWidth,
            pinChordClearanceM = 0.0,
        )

        assertTrue(audit.minShoeWheelGapM < 0.0, "shoe must be reported inside the wheel")
        assertEquals(-sagitta, audit.minShoeWheelGapM, 1e-6)
        assertTrue(audit.interpenetrates)
        assertEquals("idler", audit.worstWheelBone)
        // The pin metric is still non-negative: this is exactly the trap.
        assertEquals(0.0, audit.minPinChordClearanceM, 1e-12)
    }

    @Test
    fun theCorrectedLocusKeepsTheShoeOutOfTheWheel() {
        val count = 64
        val radius = ringRadius(count)
        val clearance = 0.002
        val pins = ringPins(count)
        // Invert the locus formula: pick the wheel radius that yields `clearance`.
        val wheelRadius = sqrt(radius * radius - (pitch / 2.0) * (pitch / 2.0)) - pinInset - clearance

        val audit = TrackedGeometryAudit.audit(
            placements = placements(pins, shoeCentreOffset()),
            wheels = listOf(wheel("idler", Vec3.ZERO, wheelRadius)),
            shoeHalfWidth = shoeHalfWidth,
            pinChordClearanceM = 0.0,
        )

        assertTrue(audit.minShoeWheelGapM > 0.0, "gap ${audit.minShoeWheelGapM}")
        assertEquals(clearance, audit.minShoeWheelGapM, 1e-6)
        assertFalse(audit.interpenetrates)
    }

    @Test
    fun aWheelThatDoesNotOverlapLaterallyIsIgnored() {
        val audit = TrackedGeometryAudit.audit(
            placements = placements(ringPins(12)),
            wheels = listOf(wheel("far", Vec3(5.0, 0.0, 0.0), 0.5)),
            shoeHalfWidth = shoeHalfWidth,
            pinChordClearanceM = 0.0,
        )
        assertEquals(0, audit.checkedPairs)
        assertEquals(Double.POSITIVE_INFINITY, audit.minShoeWheelGapM)
        assertFalse(audit.interpenetrates)
    }

    /**
     * The box offset is applied once, in the pin frame, and the measured gap
     * uses that same centre. On a straight link the arithmetic is exact.
     */
    @Test
    fun anOffsetShoeIsPlacedAndMeasuredInTheSameFrame() {
        val half = Vec3(shoeHalfWidth, thickness / 2.0, length / 2.0)
        // A straight link: pins one pitch apart along +Z, no rotation.
        val start = ClosedTrackLoopSolver.Pin(0.0, 0.0)
        val end = ClosedTrackLoopSolver.Pin(pitch, 0.0)
        val road = wheel("road", Vec3(0.0, -0.5, pitch * 0.5), 0.4)

        fun linkAt(boxCenter: Vec3) = TrackedAssemblyGeometry.place(
            pins = listOf(start, end, ClosedTrackLoopSolver.Pin(2 * pitch, 0.0), ClosedTrackLoopSolver.Pin(3 * pitch, 0.0)),
            centerX = 0.0,
            boxCenterInPinFrame = boxCenter,
            halfExtents = half,
        ).first()

        val centred = linkAt(Vec3.ZERO)
        val offset = Vec3(0.0, thickness / 2.0, 0.0)
        val shifted = linkAt(offset)

        assertEquals(centred.center.y + offset.y, shifted.center.y, 1e-12)
        assertEquals(centred.center.z + offset.z, shifted.center.z, 1e-12)
        assertEquals(offset, shifted.boxCenterInPinFrame)

        val centredGap = TrackedAssemblyGeometry.shoeWheelGap(
            centred, shoeHalfWidth, road.assemblyCenter, road.radius, road.halfWidth,
        )
        val shiftedGap = TrackedAssemblyGeometry.shoeWheelGap(
            shifted, shoeHalfWidth, road.assemblyCenter, road.radius, road.halfWidth,
        )
        // The wheel sits below the belt, so raising the shoe by half a thickness
        // must widen the gap by exactly that amount.
        assertEquals(centredGap + thickness / 2.0, shiftedGap, 1e-12)
    }

    @Test
    fun hingeAngleIsMeasuredFromThePlacedLinks() {
        val count = 8
        val placed = placements(ringPins(count))
        val expected = 360.0 / count
        assertEquals(expected, TrackedGeometryAudit.maxJointAngleDegrees(placed), 1e-3)
        assertTrue(TrackedGeometryAudit.withinLimit(Math.toRadians(expected), Math.toRadians(expected)))
        assertFalse(TrackedGeometryAudit.withinLimit(Math.toRadians(expected * 2.0), Math.toRadians(expected)))
    }

    @Test
    fun pinsAreOnePerLinkAndTheLoopClosesImplicitly() {
        val circles = listOf(
            circle(z = 1.0, y = 0.0, radius = 0.4, pinRadius = 0.45),
            circle(z = -1.0, y = 0.0, radius = 0.4, pinRadius = 0.45),
        )
        val count = ClosedTrackLoopSolver.candidateCounts(circles, pitch)
            .firstOrNull { ClosedTrackLoopSolver.solve(circles, pitch, it, 0, 0.4) != null }
        assertTrue(count != null, "a two-wheel belt must close at some count of $pitch m")
        val loop = ClosedTrackLoopSolver.solve(circles, pitch, count!!, 0, 0.4)!!
        assertEquals(count, loop.pins.size, "one pin per link, no duplicated closing pin")
        val worst = loop.pins.indices.maxOf { index ->
            val a = loop.pins[index]
            val b = loop.pins[(index + 1) % loop.pins.size]
            abs(hypot(b.z - a.z, b.y - a.y) - pitch)
        }
        assertTrue(worst < ClosedTrackLoopSolver.MAX_PITCH_ERROR_M, "every chord, including the closing one, is one pitch")
    }

    private fun circle(z: Double, y: Double, radius: Double, pinRadius: Double) =
        ClosedTrackLoopSolver.Circle(z = z, y = y, pinRadius = pinRadius, wheelRadius = radius)
}
