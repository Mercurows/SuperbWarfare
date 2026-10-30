package com.atsuishio.superbwarfare.physics.tracked

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * Pure S4 control law, without a physics world.
 *
 * These cases prove the three states stay distinct, that a right turn drives
 * the left side harder, and that a wide authored differential cannot cancel
 * the throttle. They do **not** prove traction.
 */
class TrackedDriveControllerTest {

    private fun resolve(
        throttle: Float = 0f,
        steer: Float = 0f,
        brake: Boolean = false,
        powered: Boolean = true,
        differential: Double = 1.7,
        maxSpeedMps: Double = 15.6,
    ) = TrackedDriveController.resolve(throttle, steer, brake, powered, differential, maxSpeedMps)

    @Test
    fun unpoweredIsCoast() {
        val demand = resolve(throttle = 1f, powered = false)
        assertEquals(TrackedDriveController.DriveState.COAST, demand.state)
        assertEquals(0.0, demand.left, 0.0)
        assertEquals(0.0, demand.right, 0.0)
        assertEquals(0.0, demand.targetSpeedMps, 0.0)
    }

    @Test
    fun zeroThrottleIsCoastNotBrake() {
        val demand = resolve(throttle = 0f, brake = false)
        assertEquals(TrackedDriveController.DriveState.COAST, demand.state)
    }

    @Test
    fun brakeAsksForZeroSurfaceSpeed() {
        val demand = resolve(throttle = 1f, brake = true)
        assertEquals(TrackedDriveController.DriveState.BRAKE, demand.state)
        assertEquals(0.0, demand.left, 0.0)
        assertEquals(0.0, demand.right, 0.0)
        assertEquals(0.0, demand.targetSpeedMps, 0.0)
    }

    @Test
    fun straightThrottleDrivesBothSidesForward() {
        val demand = resolve(throttle = 1f)
        assertEquals(TrackedDriveController.DriveState.DRIVE, demand.state)
        assertTrue(demand.left > 0.0 && demand.right > 0.0, "straight throttle must drive both sides")
        assertEquals(demand.left, demand.right, 1e-9)
        assertEquals(15.6, demand.targetSpeedMps, 1e-9)
        assertEquals(15.6, demand.leftTargetSpeedMps, 1e-9)
        assertEquals(15.6, demand.rightTargetSpeedMps, 1e-9)
    }

    @Test
    fun partialThrottleIsScaledOnce() {
        val demand = resolve(throttle = 0.4f, maxSpeedMps = 10.0)
        assertEquals(4.0, demand.leftTargetSpeedMps, 1e-5)
        assertEquals(4.0, demand.rightTargetSpeedMps, 1e-5)
        assertEquals(4.0, demand.targetSpeedMps, 1e-5)
    }

    @Test
    fun reverseThrottleIsNegative() {
        val demand = resolve(throttle = -1f)
        assertEquals(TrackedDriveController.DriveState.DRIVE, demand.state)
        assertTrue(demand.left < 0.0 && demand.right < 0.0, "reverse must be negative on both sides")
    }

    @Test
    fun aRightTurnDrivesTheLeftSideHarder() {
        val demand = resolve(throttle = 0.4f, steer = 1f)
        assertEquals(TrackedDriveController.DriveState.DRIVE, demand.state)
        assertTrue(demand.left > demand.right, "a right turn must drive the left side harder")
    }

    @Test
    fun aWideDifferentialCannotCancelThrottle() {
        // 99A authors 1.7. Splitting first then clamping each side independently
        // used to turn a small forward command into +1/-1 and a zero average.
        val demand = resolve(throttle = 0.15f, steer = 1f, differential = 1.7)
        assertEquals(TrackedDriveController.DriveState.DRIVE, demand.state)
        assertTrue(demand.left > 0.0, "the faster side of a right turn must stay forward")
        assertTrue(abs(demand.left) <= 1.0 + 1e-9)
        assertTrue(abs(demand.right) <= 1.0 + 1e-9)
        assertTrue(demand.normalised, "a 1.7 split of 0.15 must saturate and then be scaled")
    }

    @Test
    fun nonFiniteInputCoasts() {
        assertEquals(TrackedDriveController.DriveState.COAST, resolve(throttle = Float.NaN).state)
        assertEquals(TrackedDriveController.DriveState.COAST, resolve(steer = Float.POSITIVE_INFINITY).state)
        assertEquals(TrackedDriveController.DriveState.COAST, resolve(maxSpeedMps = Double.NaN).state)
        assertEquals(TrackedDriveController.DriveState.COAST, resolve(maxSpeedMps = 0.0).state)
    }

    // ---- MCSP-style stateful response layer --------------------------------

    private fun drivetrain(
        accel: Double? = 3.0,
        brake: Double? = 6.0,
        pivot: Double? = 1.8,
        steerTime: Double? = 1.5,
    ) = CompiledDrivetrain(
        maxDriveTorqueNm = 160_000.0,
        maxBrakeTorqueNm = 320_000.0,
        maxSpeedMps = 15.6,
        differential = 1.7,
        rollingResistance = 0.015,
        friction = 0.8,
        accelMps2 = accel,
        brakeMps2 = brake,
        pivotSpeedMps = pivot,
        steerTimeSeconds = steerTime,
    )

    @Test
    fun slewAcceleratesTowardTargetAtTheAuthoredRate() {
        // 3 m/s² for 0.1 s = 0.3 m/s.
        assertEquals(0.3, TrackedDriveController.slew(0.0, 10.0, 3.0, 6.0, 0.1), 1e-9)
        // Target closer than one step: land exactly on it.
        assertEquals(0.2, TrackedDriveController.slew(0.0, 0.2, 3.0, 6.0, 0.1), 1e-9)
    }

    @Test
    fun slewBrakesBeforeReversing() {
        // 1 m/s forward, target -10. 6 m/s² brake for 0.1 s consumes 0.6 m/s,
        // so the result is +0.4 m/s and must not cross zero yet.
        assertEquals(0.4, TrackedDriveController.slew(1.0, -10.0, 3.0, 6.0, 0.1), 1e-9)
        // Brake longer than the stop time: spend 1/6 s stopping, 0.05 s
        // accelerating backward at 3 m/s².
        assertEquals(-0.15, TrackedDriveController.slew(1.0, -10.0, 3.0, 6.0, 1.0 / 6.0 + 0.05), 1e-9)
    }

    @Test
    fun slewUsesBrakeRateWhenTheTargetIsCloserToZero() {
        // Decelerating from 10 toward 5 is a speed decrease: brake rate applies.
        val result = TrackedDriveController.slew(10.0, 5.0, 3.0, 6.0, 0.1)
        assertEquals(9.4, result, 1e-9)
    }

    @Test
    fun theSteerLeverTakesTheAuthoredTimeToReachFullLock() {
        val controller = TrackedDriveController.Controller()
        val drive = drivetrain(accel = null, brake = null)
        var demand = TrackedDriveController.Demand.COAST
        repeat(15) {
            demand = controller.resolve(0f, 1f, false, true, drive, 0.1)
        }
        // 15 x 0.1 s = 1.5 s = full steer time. With no throttle the target is
        // purely the pivot speed, so the lever is observable in the demand.
        assertEquals(1.0, controller.lever(), 1e-9)
        assertEquals(1.8, demand.leftTargetSpeedMps, 1e-9)
        assertEquals(-1.8, demand.rightTargetSpeedMps, 1e-9)
    }

    @Test
    fun pivotSpeedBuildsTheTargetAroundTheThrottleSpeed() {
        val controller = TrackedDriveController.Controller()
        val drive = drivetrain(accel = null, brake = null, steerTime = null)
        val demand = controller.resolve(0.2f, 1f, false, true, drive, 0.1)
        // 0.2 * 15.6 = 3.12; pivot 1.8 -> left 4.92, right 1.32, both under max.
        assertEquals(4.92, demand.leftTargetSpeedMps, 1e-6)
        assertEquals(1.32, demand.rightTargetSpeedMps, 1e-6)
    }

    @Test
    fun pivotSpeedTargetsAreScaledOnlyWhenThePeakExceedsMaxSpeed() {
        val controller = TrackedDriveController.Controller()
        val drive = drivetrain(accel = null, brake = null, steerTime = null)
        val demand = controller.resolve(1f, 1f, false, true, drive, 0.1)
        // 15.6 + 1.8 = 17.4 over 15.6: scale the whole pair by 15.6/17.4.
        val scale = 15.6 / 17.4
        assertEquals(15.6, demand.leftTargetSpeedMps, 1e-6)
        assertEquals(13.8 * scale, demand.rightTargetSpeedMps, 1e-6)
        assertTrue(demand.normalised)
    }

    @Test
    fun aPivotTurnWithoutThrottleDrivesTheSidesOpposite() {
        val controller = TrackedDriveController.Controller()
        val drive = drivetrain(accel = null, brake = null, steerTime = null)
        val demand = controller.resolve(0f, 1f, false, true, drive, 0.1)
        assertEquals(1.8, demand.leftTargetSpeedMps, 1e-9)
        assertEquals(-1.8, demand.rightTargetSpeedMps, 1e-9)
        assertEquals(1.8, demand.targetSpeedMps, 1e-9)
    }

    @Test
    fun slippingMatchesMcspSurfaceVersusCraftComparison() {
        // Commanded forward, belt running 0.6 m/s faster than the craft: slipping.
        assertTrue(TrackedDriveController.slipping(5.0, 5.6, 5.0))
        // Exactly at the 0.5 m/s threshold is not over it.
        assertFalse(TrackedDriveController.slipping(5.0, 5.5, 5.0))
        // Reverse uses the commanded sign, not an absolute value.
        assertTrue(TrackedDriveController.slipping(-5.0, -5.6, -5.0))
        assertFalse(TrackedDriveController.slipping(0.0, 10.0, 0.0))
        assertFalse(TrackedDriveController.slipping(Double.NaN, 1.0, 0.0))
    }

    @Test
    fun tractionGripIsAFractionOfWhatTheContactCanCarry() {
        // 0.5 of the friction-limited demand.
        assertEquals(0.5, TrackedDriveController.tractionGripFraction(5_000.0, 10_000.0, 1.0), 1e-9)
        // Authored friction caps the available force, it does not scale up.
        assertEquals(1.0, TrackedDriveController.tractionGripFraction(20_000.0, 10_000.0, 0.8), 1e-9)
        // No load or no friction is not evidence for a reduction.
        assertEquals(1.0, TrackedDriveController.tractionGripFraction(1_000.0, 0.0, 0.8), 1e-9)
        assertEquals(1.0, TrackedDriveController.tractionGripFraction(1_000.0, 10_000.0, 0.0), 1e-9)
        assertEquals(0.0, TrackedDriveController.tractionGripFraction(0.0, 10_000.0, 0.8), 1e-9)
    }

    @Test
    fun aBrakeRequestDoesNotReverseThroughTheRateLimit() {
        val controller = TrackedDriveController.Controller()
        val drive = drivetrain(steerTime = null)
        // Build 10 m/s of command first.
        repeat(100) { controller.resolve(1f, 0f, false, true, drive, 0.1) }
        assertEquals(15.6, controller.leftSpeedMps(), 1e-6)
        // One 0.1 s brake step removes only brakeRate * dt = 0.6 m/s.
        val braked = controller.resolve(1f, 0f, true, true, drive, 0.1)
        assertEquals(TrackedDriveController.DriveState.BRAKE, braked.state)
        assertEquals(15.0, controller.leftSpeedMps(), 1e-6)
        assertTrue(controller.leftSpeedMps() > 0.0, "braking must not cross zero in one step")
    }

    @Test
    fun aControllerWithoutRatesKeepsTheHistoricalImmediateTarget() {
        val controller = TrackedDriveController.Controller()
        val drive = drivetrain(accel = null, brake = null, pivot = null, steerTime = null)
        val demand = controller.resolve(1f, 0f, false, true, drive, 0.1)
        assertEquals(15.6, demand.leftTargetSpeedMps, 1e-9)
        assertEquals(15.6, demand.rightTargetSpeedMps, 1e-9)
    }
}
