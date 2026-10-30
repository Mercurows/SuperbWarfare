package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

/**
 * Field-level contract for a tracked mechanism definition.
 *
 * These checks run without a model file, so a bad number is reported as itself
 * instead of surfacing later as a confusing geometry or solver failure.
 */
class TrackedDefinitionValidationTest {

    private fun definition(): TrackedVehicleDefinition {
        val base = TrackedVehicleDefinition(
            model = "superbwarfare:models/bedrock/vehicle/ztz_99a.geo.json",
            massKg = 55_000.0,
            chassisOrigin = Vec3(0.0, 1.0938, -0.094),
            track = TrackMechanismDefinition(
                pitch = 0.23307,
                pinInset = 0.048828125,
                angularLimitDegrees = 40.0,
                linkCount = 79,
                clearance = 0.002,
                linkMass = 18.0,
            ),
        )
        base.left.linkTemplateBone = "trackRotL0"
        base.right.linkTemplateBone = "trackRotR0"
        for (side in TrackedSide.entries) {
            val definition = base.side(side)
            definition.wheels += TrackWheelDefinition(
                bone = "wheel${side.name.first()}0",
                role = TrackWheelRole.IDLER_GUIDE,
                loadShare = 0.0,
                tensioner = TrackTensionerDefinition(),
                mass = 400.0,
                radius = 0.351562,
                halfWidth = 0.2,
                wrap = TrackWrap.OUTSIDE,
            )
            for (index in 1..6) {
                definition.wheels += TrackWheelDefinition(
                    bone = "wheel${side.name.first()}$index",
                    role = TrackWheelRole.SUPPORT,
                    loadShare = 1.0,
                    mass = 900.0,
                    radius = 0.53125,
                    halfWidth = 0.2,
                    wrap = TrackWrap.OUTSIDE,
                )
            }
            definition.wheels += TrackWheelDefinition(
                bone = "wheel${side.name.first()}7",
                role = TrackWheelRole.DRIVE_GUIDE,
                loadShare = 0.0,
                mass = 400.0,
                radius = 0.351562,
                halfWidth = 0.2,
                wrap = TrackWrap.OUTSIDE,
            )
            definition.beltOrder = definition.wheels.map { it.bone }.toMutableList()
            definition.runMassBudget = 1422.0
        }
        return base
    }

    private fun errors(definition: TrackedVehicleDefinition): List<TrackedAuditFinding> =
        TrackedDefinitionValidator.validate(definition).filter { it.severity == AuditSeverity.ERROR }

    @Test
    fun aWellFormedDefinitionHasNoErrors() {
        val findings = TrackedDefinitionValidator.validate(definition())
        assertEquals(emptyList<TrackedAuditFinding>(), findings.filter { it.severity == AuditSeverity.ERROR })
    }

    @Test
    fun nonFiniteAndOutOfRangeNumbersAreRejected() {
        assertTrue(errors(definition().also { it.massKg = Double.NaN }).isNotEmpty())
        assertTrue(errors(definition().also { it.renderScale = Double.POSITIVE_INFINITY }).isNotEmpty())
        assertTrue(errors(definition().also { it.chassisOrigin = Vec3(Double.NaN, 0.0, 0.0) }).isNotEmpty())
        assertTrue(errors(definition().also { it.track.pitch = -1.0 }).isNotEmpty())
        assertTrue(errors(definition().also { it.track.pinInset = -0.001 }).isNotEmpty())
        assertTrue(errors(definition().also { it.track.clearance = Double.NaN }).isNotEmpty())
        assertTrue(errors(definition().also { it.track.linkMass = 0.0 }).isNotEmpty())
        assertTrue(errors(definition().also { it.track.linkCount = -1 }).isNotEmpty())
        assertTrue(errors(definition().also { it.track.angularLimitDegrees = 0.0 }).isNotEmpty())
    }

    @Test
    fun invalidSuspensionAndTensionerNumbersAreRejected() {
        val base = definition()
        base.left.suspension.compressionDampingRatio = -0.1
        assertTrue(errors(base).any { it.code.contains("compression") })

        val rebound = definition()
        rebound.left.suspension.reboundDampingRatio = Double.NaN
        assertTrue(errors(rebound).any { it.code.contains("rebound") })

        val frequency = definition()
        frequency.left.suspension.frequency = 0.0
        assertTrue(errors(frequency).any { it.code.contains("suspension") })

        val bump = definition()
        bump.left.suspension.bumpStopStart = 1.5
        assertTrue(errors(bump).any { it.code.contains("suspension") })

        val travel = definition()
        travel.left.wheels.first().tensioner = TrackTensionerDefinition(travel = 0.0)
        assertTrue(errors(travel).any { it.code.contains("tensioner.travel") })

        val preload = definition()
        preload.left.wheels.first().tensioner = TrackTensionerDefinition(preload = -1.0)
        assertTrue(errors(preload).any { it.code.contains("tensioner.preload") })
    }

    @Test
    fun aTensionerOnANonIdlerIsRejected() {
        val definition = definition()
        definition.left.wheels[3].tensioner = TrackTensionerDefinition()
        assertTrue(
            errors(definition).any { it.code.contains("tensioner.role") },
            "only an IdlerGuide may carry a tensioner",
        )
    }

    @Test
    fun duplicateAndCrossSideBoneBindingsAreRejected() {
        val sameSide = definition()
        sameSide.left.wheels[3].bone = sameSide.left.wheels[2].bone
        assertTrue(errors(sameSide).any { it.code.contains("duplicate") })

        val crossSide = definition()
        crossSide.right.wheels[3].bone = crossSide.left.wheels[3].bone
        assertTrue(errors(crossSide).any { it.code.contains("duplicate") })
    }

    @Test
    fun roleCountsMustBeSatisfiable() {
        val noIdler = definition()
        noIdler.left.wheels.first().role = TrackWheelRole.SUPPORT
        noIdler.left.wheels.first().loadShare = 1.0
        assertTrue(errors(noIdler).any { it.code.contains("idler") })

        val noDrive = definition()
        noDrive.right.wheels.last().role = TrackWheelRole.SUPPORT
        noDrive.right.wheels.last().loadShare = 1.0
        assertTrue(errors(noDrive).any { it.code.contains("drive") })
    }

    @Test
    fun aCantedAxleOrSuspensionIsRejectedInsteadOfProjected() {
        val axle = definition()
        axle.left.wheels[1].axleDirection = Vec3(1.0, 0.2, 0.0).normalize()
        assertTrue(errors(axle).any { it.code.contains("axle") })

        val suspension = definition()
        suspension.left.suspension.direction = Vec3(0.0, 1.0, 0.3).normalize()
        assertTrue(errors(suspension).any { it.code.contains("direction") })

        val zero = definition()
        zero.left.wheels[1].axleDirection = Vec3.ZERO
        assertTrue(errors(zero).any { it.code.contains("axle") })
    }

    @Test
    fun drivetrainNumbersAreValidated() {
        assertTrue(errors(definition().also { it.drivetrain.maxDriveTorque = 0.0 }).isNotEmpty())
        assertTrue(errors(definition().also { it.drivetrain.maxSpeed = Double.NaN }).isNotEmpty())
        assertTrue(errors(definition().also { it.drivetrain.rollingResistance = 2.0 }).isNotEmpty())
        assertTrue(errors(definition().also { it.drivetrain.friction = -1.0 }).isNotEmpty())
    }

    @Test
    fun mcspStyleResponseBoundsAreValidatedButOptional() {
        // Absent is the historical contract and must stay valid.
        assertEquals(emptyList<TrackedAuditFinding>(), errors(definition()))
        assertTrue(errors(definition().also { it.drivetrain.accelMps2 = -1.0 }).isNotEmpty())
        assertTrue(errors(definition().also { it.drivetrain.brakeMps2 = Double.NaN }).isNotEmpty())
        assertTrue(errors(definition().also { it.drivetrain.pivotSpeedMps = -0.1 }).isNotEmpty())
        assertTrue(errors(definition().also { it.drivetrain.steerTimeSeconds = 0.0 }).isNotEmpty())
        // MCSP's Accel/Brake are strictly positive rates. Zero is not
        // "unlimited"; it is ambiguous and must be rejected. PivotSpeed may be
        // zero because a vehicle without a neutral-turn capability is valid.
        assertTrue(errors(definition().also { it.drivetrain.accelMps2 = 0.0 }).isNotEmpty())
        assertTrue(errors(definition().also { it.drivetrain.brakeMps2 = 0.0 }).isNotEmpty())
        assertEquals(
            emptyList<TrackedAuditFinding>(),
            errors(definition().also { it.drivetrain.pivotSpeedMps = 0.0 }),
        )
    }

    @Test
    fun directSpringAndDamperAreValidatedButOptional() {
        assertTrue(errors(definition().also { it.left.suspension.springNPerM = 0.0 }).isNotEmpty())
        assertTrue(errors(definition().also { it.left.suspension.damperNsPerM = -1.0 }).isNotEmpty())
        assertEquals(
            emptyList<TrackedAuditFinding>(),
            errors(definition().also {
                it.left.suspension.springNPerM = 320_000.0
                it.left.suspension.damperNsPerM = 0.0
            }),
        )
    }

    @Test
    fun anAutoLinkCountIsAllowedButReported() {
        val auto = definition().also { it.track.linkCount = 0 }
        val findings = TrackedDefinitionValidator.validate(auto)
        assertFalse(findings.any { it.severity == AuditSeverity.ERROR })
        assertNotNull(findings.singleOrNull { it.code == "link-count" && it.severity == AuditSeverity.WARN })
    }

    @Test
    fun boxAndCylinderInertiaFollowTheClosedForm() {
        val half = Vec3(0.5, 0.25, 2.0)
        val box = TrackedMassProperties.box("box", 120.0, half)
        assertEquals(120.0 / 12.0 * (0.5 * 0.5 + 4.0 * 4.0), box.inertiaKgM2.x, 1e-9)
        assertEquals(120.0 / 12.0 * (1.0 * 1.0 + 4.0 * 4.0), box.inertiaKgM2.y, 1e-9)
        assertEquals(120.0 / 12.0 * (1.0 * 1.0 + 0.5 * 0.5), box.inertiaKgM2.z, 1e-9)

        val cylinder = TrackedMassProperties.cylinder("w", 900.0, 0.53125, 0.2, Vec3(1.0, 0.0, 0.0))
        assertEquals(0.5 * 900.0 * 0.53125 * 0.53125, cylinder.inertiaKgM2.x, 1e-9)
        val perpendicular = 900.0 * (3.0 * 0.53125 * 0.53125 + 0.4 * 0.4) / 12.0
        assertEquals(perpendicular, cylinder.inertiaKgM2.y, 1e-9)
        assertEquals(perpendicular, cylinder.inertiaKgM2.z, 1e-9)
    }
}
