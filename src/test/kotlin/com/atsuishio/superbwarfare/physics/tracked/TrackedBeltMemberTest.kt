package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The belt's member list, wrap values and plane are the physical topology.
 *
 * These are the cases that a "it compiles, therefore it is right" test misses:
 * a missing order, a dropped member, an implicit wrap and a belt plane derived
 * from wheel averages all produce plausible numbers and a wrong mechanism.
 */
class TrackedBeltMemberTest {

    private fun wheel(
        index: Int,
        z: Double,
        radius: Double = 0.5,
        mass: Double = 900.0,
        wrap: TrackWrap = TrackWrap.OUTSIDE,
    ) = CompiledWheel(
        side = TrackedSide.LEFT,
        role = TrackWheelRole.SUPPORT,
        bone = "w$index",
        index = index,
        bindCenter = Vec3(0.0, 0.0, z),
        assemblyCenter = Vec3(0.0, 0.0, z),
        radius = radius,
        halfWidth = 0.2,
        axleDirection = Vec3(1.0, 0.0, 0.0),
        massKg = mass,
        suspension = CompiledSuspension(
            travelUp = 0.18, travelDown = 0.24, frequency = 2.2,
            stiffnessNPerM = 1.0, compressionDampingNsPerM = 1.0, reboundDampingNsPerM = 1.0,
            maxForceN = 400_000.0, bumpStopStart = 0.85, bumpStopExponent = 2.0,
            direction = Vec3(0.0, 1.0, 0.0), stationMassKg = 1000.0, staticCompressionM = 0.05,
        ),
        wrap = wrap,
    )

    private fun belt(width: Double = 0.4, thickness: Double = 0.1, centerX: Double = 0.0) =
        CompiledBeltGeometry(
            centerPlaneX = centerX,
            lateralAxis = Vec3(1.0, 0.0, 0.0),
            widthM = width,
            thicknessM = thickness,
            sizeSource = "test",
        )

    private fun members(vararg wraps: TrackWrap): List<CompiledBeltMember> =
        wraps.mapIndexed { index, wrap ->
            CompiledBeltMember(wheelIndex = index, ordinal = index, bone = "w$index", wrap = wrap)
        }

    /**
     * A two-member belt must produce two runs, one on each side of the pair.
     *
     * This is the case a centroid-based side choice cannot produce: the centroid
     * of two centres is their midpoint, so the "away" vector is zero for both
     * directions.
     */
    @Test
    fun aTwoMemberBeltProducesTwoOppositeRuns() {
        val wheels = listOf(wheel(0, z = -2.0), wheel(1, z = 2.0)).associateBy { it.index }
        val runs = TrackedRunGeometry.compile(
            members = members(TrackWrap.OUTSIDE, TrackWrap.OUTSIDE),
            wheels = wheels,
            belt = belt(),
            runMasses = listOf(10.0, 10.0),
        )
        assertEquals(2, runs.size)
        assertTrue(runs[1].closesLoop, "the second run closes a two-member belt")
        assertEquals(0, runs[0].firstWheelIndex)
        assertEquals(1, runs[0].secondWheelIndex)
        assertEquals(1, runs[1].firstWheelIndex)
        assertEquals(0, runs[1].secondWheelIndex)

        // The two runs sit on opposite sides of the wheel-centre line.
        val firstOffset = runs[0].center.y - 0.0
        val secondOffset = runs[1].center.y - 0.0
        assertTrue(firstOffset * secondOffset < 0.0, "runs must be on opposite sides, were $firstOffset / $secondOffset")
        assertNotEquals(runs[0].center, runs[1].center)
    }

    @Test
    fun anEmptyOrShortBeltIsRejected() {
        val wheels = listOf(wheel(0, z = -2.0)).associateBy { it.index }
        assertThrows(IllegalArgumentException::class.java) {
            TrackedRunGeometry.compile(
                members = listOf(CompiledBeltMember(0, 0, "w0", TrackWrap.OUTSIDE)),
                wheels = wheels,
                belt = belt(),
                runMasses = listOf(10.0),
            )
        }
    }

    @Test
    fun duplicateWheelsInTheOrderAreRejected() {
        val wheels = listOf(wheel(0, z = -2.0), wheel(1, z = 2.0)).associateBy { it.index }
        assertThrows(IllegalArgumentException::class.java) {
            TrackedRunGeometry.compile(
                members = listOf(
                    CompiledBeltMember(0, 0, "w0", TrackWrap.OUTSIDE),
                    CompiledBeltMember(0, 1, "w0", TrackWrap.OUTSIDE),
                ),
                wheels = wheels,
                belt = belt(),
                runMasses = listOf(10.0, 10.0),
            )
        }
    }

    @Test
    fun nonSequentialOrdinalsAreRejected() {
        val wheels = listOf(wheel(0, z = -2.0), wheel(1, z = 2.0)).associateBy { it.index }
        assertThrows(IllegalArgumentException::class.java) {
            TrackedRunGeometry.compile(
                members = listOf(
                    CompiledBeltMember(0, 1, "w0", TrackWrap.OUTSIDE),
                    CompiledBeltMember(1, 0, "w1", TrackWrap.OUTSIDE),
                ),
                wheels = wheels,
                belt = belt(),
                runMasses = listOf(10.0, 10.0),
            )
        }
    }

    @Test
    fun aMissingRunMassIsRejected() {
        val wheels = listOf(wheel(0, z = -2.0), wheel(1, z = 2.0)).associateBy { it.index }
        assertThrows(IllegalArgumentException::class.java) {
            TrackedRunGeometry.compile(
                members = members(TrackWrap.OUTSIDE, TrackWrap.OUTSIDE),
                wheels = wheels,
                belt = belt(),
                runMasses = listOf(10.0),
            )
        }
    }

    /**
     * Every supported wrap combination must produce a tangent, and the contact
     * points must be perpendicular to the segment.
     *
     * The equation is `n · (C_j - C_i) = rho_i - rho_j`, which is the one the
     * signed-radius construction solves. A construction that puts both contact
     * points on the same side fails this for the mixed and inside cases.
     */
    @Test
    fun allFourWrapCombinationsProduceAPerpendicularTangent() {
        val cases = listOf(
            TrackWrap.OUTSIDE to TrackWrap.OUTSIDE,
            TrackWrap.OUTSIDE to TrackWrap.INSIDE,
            TrackWrap.INSIDE to TrackWrap.OUTSIDE,
            TrackWrap.INSIDE to TrackWrap.INSIDE,
        )
        for ((firstWrap, secondWrap) in cases) {
            val wheels = listOf(
                wheel(0, z = -3.0, radius = 0.5, wrap = firstWrap),
                wheel(1, z = 3.0, radius = 0.5, wrap = secondWrap),
            ).associateBy { it.index }
            val geometry = belt()
            val runs = TrackedRunGeometry.compile(
                members = members(firstWrap, secondWrap),
                wheels = wheels,
                belt = geometry,
                runMasses = listOf(10.0, 10.0),
            )
            val run = runs[0]
            val first = wheels.getValue(0)
            val second = wheels.getValue(1)
            val angle = angleOf(run.rotation).toDouble()
            val firstAnchor = run.center.add(
                TrackedAssemblyGeometry.rotateAboutX(run.first.anchorInRun, angleOf(run.rotation)),
            )
            val secondAnchor = run.center.add(
                TrackedAssemblyGeometry.rotateAboutX(run.second.anchorInRun, angleOf(run.rotation)),
            )
            // The anchors are the wheel centres, by design. The tangent invariant is
            // therefore n . (C2 - C1) = rho1 - rho2, not perpendicularity: the
            // contact points sit at C + rho*n and only *those* are perpendicular.
            val segment = secondAnchor.subtract(firstAnchor)
            val normal = run.second.lockedNormalLocal!!
            val normalWorld = TrackedAssemblyGeometry.rotateAboutX(normal, angleOf(run.rotation)).normalize()
            val offset = geometry.runCenterOffset(first.radius)
            val offsetSecond = geometry.runCenterOffset(second.radius)
            val rhoFirst = if (firstWrap == TrackWrap.OUTSIDE) offset else -offset
            val rhoSecond = if (secondWrap == TrackWrap.OUTSIDE) offsetSecond else -offsetSecond
            assertEquals(
                rhoFirst - rhoSecond,
                segment.dot(normalWorld),
                1.0e-6,
                "$firstWrap->$secondWrap: n . (C2 - C1) must equal rho1 - rho2",
            )
            // And the contact points themselves must be perpendicular to n.
            val contactFirst = first.assemblyCenter.add(normalWorld.scale(rhoFirst))
            val contactSecond = second.assemblyCenter.add(normalWorld.scale(rhoSecond))
            assertEquals(
                0.0,
                contactSecond.subtract(contactFirst).dot(normalWorld),
                1.0e-6,
                "$firstWrap->$secondWrap: contact line must be perpendicular to n",
            )
            assertTrue(
                run.lengthM > 0.0 && run.lengthM.isFinite(),
                "$firstWrap->$secondWrap produced length ${run.lengthM}",
            )
            assertTrue(
                run.second.lockedNormalLocal!!.length() > 0.0,
                "$firstWrap->$secondWrap produced a zero second-end normal",
            )
            // The wheel is still the reference for the anchors.
            assertEquals(
                0.0,
                secondAnchor.distanceTo(second.assemblyCenter),
                1.0e-6,
                "$firstWrap->$secondWrap second anchor",
            )
            assertEquals(
                0.0,
                firstAnchor.distanceTo(first.assemblyCenter),
                1.0e-6,
                "$firstWrap->$secondWrap first anchor",
            )
        }
    }

    @Test
    fun overlappingWheelsAreRejectedRatherThanProducingANegativeTangent() {
        val wheels = listOf(
            wheel(0, z = -1.0, radius = 2.0),
            wheel(1, z = 1.0, radius = 2.0),
        ).associateBy { it.index }
        assertThrows(IllegalArgumentException::class.java) {
            TrackedRunGeometry.compile(
                members = members(TrackWrap.OUTSIDE, TrackWrap.OUTSIDE),
                wheels = wheels,
                belt = belt(),
                runMasses = listOf(10.0, 10.0),
            )
        }
    }

    @Test
    fun theBeltPlaneIsUsedInsteadOfTheWheelMean() {
        // Wheels sit off the plane on purpose: a run centred on their mean would
        // land 0.2 m away from the audited belt plane.
        val wheels = listOf(
            wheel(0, z = -2.0).copy(assemblyCenter = Vec3(0.2, 0.0, -2.0), bindCenter = Vec3(0.2, 0.0, -2.0)),
            wheel(1, z = 2.0).copy(assemblyCenter = Vec3(0.2, 0.0, 2.0), bindCenter = Vec3(0.2, 0.0, 2.0)),
        ).associateBy { it.index }
        val runs = TrackedRunGeometry.compile(
            members = members(TrackWrap.OUTSIDE, TrackWrap.OUTSIDE),
            wheels = wheels,
            belt = belt(centerX = 0.0),
            runMasses = listOf(10.0, 10.0),
        )
        for (run in runs) {
            assertEquals(0.0, run.center.x, 1.0e-9, "run ${run.index} must use the belt plane")
        }
        // The lateral difference is still represented, in the anchors.
        val firstAnchor = runs[0].first.anchorInRun
        assertEquals(0.2, firstAnchor.x, 1.0e-9, "wheel offset must be kept in the run-local anchor")
        assertTrue(runs[0].halfExtents.x > 0.0, "run box must keep the belt width")
    }

    @Test
    fun runMassAllocationIsProportionalAndConservesTheBudget() {
        val wheels = listOf(
            wheel(0, z = -2.0, mass = 400.0),
            wheel(1, z = 2.0, mass = 900.0),
        ).associateBy { it.index }
        val allocation = TrackedMassAllocator.allocateRuns(
            members(TrackWrap.OUTSIDE, TrackWrap.OUTSIDE),
            wheels,
            1000.0,
        )
        assertEquals(1000.0, allocation.sum(), 1e-9)
        // Both pairs contain the same two wheels, so both runs get half.
        assertEquals(500.0, allocation[0], 1e-9)
        assertEquals(500.0, allocation[1], 1e-9)
    }

    @Test
    fun anAsymmetricBeltGivesTheHeavyPairMoreMass() {
        val wheels = listOf(
            wheel(0, z = -3.0, mass = 100.0),
            wheel(1, z = 0.0, mass = 900.0),
            wheel(2, z = 3.0, mass = 100.0),
        ).associateBy { it.index }
        val allocation = TrackedMassAllocator.allocateRuns(
            members(TrackWrap.OUTSIDE, TrackWrap.OUTSIDE, TrackWrap.OUTSIDE),
            wheels,
            1000.0,
        )
        // Pairs: (100+900)=1000, (900+100)=1000, (100+100)=200.
        assertEquals(1000.0, allocation.sum(), 1e-9)
        assertEquals(1000.0 / 2200.0 * 1000.0, allocation[0], 1e-9)
        assertEquals(200.0 / 2200.0 * 1000.0, allocation[2], 1e-9)
        assertTrue(allocation[0] > allocation[2], "the heavy pairs must carry more run mass")
    }

    private fun angleOf(rotation: CompiledRotation): Float =
        2f * kotlin.math.atan2(rotation.x, rotation.w)
}
