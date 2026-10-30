package com.atsuishio.superbwarfare.physics.tracked

import kotlin.math.abs
import kotlin.math.max

/**
 * Pure drive/brake/coast policy for one tracked vehicle.
 *
 * This object decides **what the drivetrain is asked to do**; it does not touch
 * Bullet. Separating the two is what lets the control law be tested without a
 * physics world, and it keeps the runtime from smuggling a chassis or wheel
 * force in as if it were contact drive.
 *
 * Three states are distinct and must stay distinct:
 *
 * - [DriveState.DRIVE]: the driver asked for longitudinal motion; the demand is
 *   a target surface speed, not a force.
 * - [DriveState.BRAKE]: the driver asked to stop; the target is zero, and the
 *   controller must not reverse through zero.
 * - [DriveState.COAST]: no driver demand; nothing is commanded, so the vehicle
 *   keeps rolling until real contact losses stop it.
 *
 * Steering is a differential on the two side demands. It is applied before the
 * per-side normalisation so a large authored differential cannot cancel the
 * throttle: normalising each side first and averaging afterwards would let a
 * `+1.25 / -1.25` split turn a forward command into zero forward demand.
 */
object TrackedDriveController {

    enum class DriveState { DRIVE, BRAKE, COAST }

    /**
     * MCSP-style rate limit: move [current] toward [target] without exceeding
     * [accel] or [brake], and without reversing through zero in one step.
     *
     * This mirrors `ChassisDrivetrain.slew`: a sign change first spends time
     * decelerating to zero at the brake rate, then the remaining time
     * accelerates toward the target. If the target is closer to zero than the
     * current speed, the brake rate applies; otherwise the acceleration rate.
     * Units are m/s and m/s². Not a force and never written to a body directly.
     */
    internal fun slew(
        current: Double,
        target: Double,
        accelMps2: Double,
        brakeMps2: Double,
        dtSeconds: Double,
    ): Double {
        if (!current.isFinite() || !target.isFinite() ||
            !accelMps2.isFinite() || accelMps2 <= 0.0 ||
            !brakeMps2.isFinite() || brakeMps2 <= 0.0 ||
            !dtSeconds.isFinite() || dtSeconds <= 0.0
        ) {
            return target
        }
        var speed = current
        var remaining = dtSeconds
        if (speed != 0.0 && target * speed < 0.0) {
            val stopSeconds = abs(speed) / brakeMps2
            if (stopSeconds >= remaining) {
                return speed - signOf(speed) * brakeMps2 * remaining
            }
            remaining -= stopSeconds
            speed = 0.0
        }
        val rate = if (abs(target) < abs(speed)) brakeMps2 else accelMps2
        val delta = target - speed
        val step = rate * remaining
        return if (abs(delta) <= step) target else speed + signOf(delta) * step
    }

    private fun signOf(value: Double): Double = when {
        value > 0.0 -> 1.0
        value < 0.0 -> -1.0
        else -> 0.0
    }

    /**
     * MCSP's slip test: the belt surface is running faster than the craft in the
     * commanded direction by more than [SLIP_MPS]. It is an observation used to
     * bound how much pull a spinning slot may claim, not an error to null out.
     *
     * Left/right slots are not modelled here because this project commands one
     * slot per side; the sign convention is the same.
     */
    internal fun slipping(
        commandedSurfaceSpeedMps: Double,
        actualSurfaceSpeedMps: Double,
        craftSpeedMps: Double,
        thresholdMps: Double = SLIP_MPS,
    ): Boolean {
        if (!commandedSurfaceSpeedMps.isFinite() || !actualSurfaceSpeedMps.isFinite() ||
            !craftSpeedMps.isFinite() || !thresholdMps.isFinite() || thresholdMps < 0.0
        ) {
            return false
        }
        return (actualSurfaceSpeedMps - craftSpeedMps) * signOf(commandedSurfaceSpeedMps) > thresholdMps
    }

    /** MCSP `SLIP_MPS`; a diagnostic band, not an acceptance limit. */
    const val SLIP_MPS = 0.5

    /**
     * How much of a commanded surface speed a slipping slot may still claim,
     * given the measured longitudinal traction and the normal load it carried.
     *
     * This is the MCSP `slotGrip = |traction|` idea expressed as a ratio: a slot
     * already spinning may only claim the fraction of the demanded force the
     * contact actually delivered. It returns 1 when there is no evidence to
     * reduce the command, and never raises it. `tractionN` and `supportedN` are
     * forces, so the ratio is dimensionless.
     */
    internal fun tractionGripFraction(
        tractionN: Double,
        supportedN: Double,
        friction: Double,
    ): Double {
        if (!tractionN.isFinite() || !supportedN.isFinite() || supportedN <= 0.0 ||
            !friction.isFinite() || friction <= 0.0
        ) {
            return 1.0
        }
        val available = abs(tractionN) / (supportedN * friction)
        return available.coerceIn(0.0, 1.0)
    }

    /**
     * Stateful MCSP-style drive policy.
     *
     * The stateless [resolve] is still the contract for a single isolated
     * command. This class adds the two pieces MCSP keeps across substeps:
     * the steering lever (`lever += clamp(steer - lever, ±dt/steerTime)`) and
     * the per-side speed slew. It is deliberately still a *target* generator:
     * the contact layer is the only place that turns the target into motion.
     */
    class Controller {
        private var lever = 0.0
        private var leftSpeedMps = 0.0
        private var rightSpeedMps = 0.0

        /** Current commanded surface speed, m/s. Diagnostics only, not traction. */
        fun leftSpeedMps(): Double = leftSpeedMps
        fun rightSpeedMps(): Double = rightSpeedMps
        fun lever(): Double = lever

        fun reset() {
            lever = 0.0
            leftSpeedMps = 0.0
            rightSpeedMps = 0.0
        }

        /**
         * Advance one substep.
         *
         * [actualLeftMps]/[actualRightMps] are belt surface speeds. They are
         * used only while coasting, to follow the real craft speed as MCSP does
         * in free roll, instead of inventing a zero-speed state.
         */
        fun resolve(
            throttle: Float,
            steer: Float,
            brake: Boolean,
            powered: Boolean,
            drivetrain: CompiledDrivetrain,
            dtSeconds: Double,
            actualLeftMps: Double = Double.NaN,
            actualRightMps: Double = Double.NaN,
        ): Demand {
            val maxSpeed = drivetrain.maxSpeedMps
            if (!throttle.isFinite() || !steer.isFinite() ||
                !maxSpeed.isFinite() || maxSpeed <= 0.0 ||
                !dtSeconds.isFinite() || dtSeconds <= 0.0
            ) {
                return coast(actualLeftMps, actualRightMps)
            }
            if (!powered) return coast(actualLeftMps, actualRightMps)

            val leverLimit = drivetrain.steerTimeSeconds?.let { time ->
                if (time.isFinite() && time > 0.0) (dtSeconds / time).coerceIn(0.0, 1.0) else 1.0
            } ?: 1.0
            lever += (steer.coerceIn(-1f, 1f).toDouble() - lever).coerceIn(-leverLimit, leverLimit)
            lever = lever.coerceIn(-1.0, 1.0)

            if (brake) {
                val brakeRate = drivetrain.brakeMps2
                val accelRate = drivetrain.accelMps2
                if (brakeRate != null && accelRate != null) {
                    leftSpeedMps = slew(leftSpeedMps, 0.0, accelRate, brakeRate, dtSeconds)
                    rightSpeedMps = slew(rightSpeedMps, 0.0, accelRate, brakeRate, dtSeconds)
                } else {
                    // No authored rate: keep the historical instantaneous stop
                    // target instead of inventing a deceleration.
                    leftSpeedMps = 0.0
                    rightSpeedMps = 0.0
                }
                return Demand(DriveState.BRAKE, 0.0, 0.0, 0.0)
            }

            val center = throttle.coerceIn(-1f, 1f).toDouble()
            val pivot = drivetrain.pivotSpeedMps
            val leftTarget: Double
            val rightTarget: Double
            val normalised: Boolean
            if (pivot != null && pivot.isFinite() && pivot >= 0.0) {
                // MCSP: target = throttle * maxSpeed ± steering lever * pivotSpeed,
                // then scale both sides only when the peak exceeds maxSpeed.
                val base = center * maxSpeed
                val turn = lever * pivot
                val rawLeft = base + turn
                val rawRight = base - turn
                val peak = max(abs(rawLeft), abs(rawRight))
                val scale = if (peak > maxSpeed) maxSpeed / peak else 1.0
                leftTarget = rawLeft * scale
                rightTarget = rawRight * scale
                normalised = scale < 1.0
            } else {
                // Existing SBW contract: the differential is a normalised split.
                val turn = (lever * drivetrain.differential).coerceIn(-1.5, 1.5)
                val rawLeft = center + turn
                val rawRight = center - turn
                val peak = max(abs(rawLeft), abs(rawRight))
                val scale = if (peak > 1.0) 1.0 / peak else 1.0
                leftTarget = rawLeft * scale * maxSpeed
                rightTarget = rawRight * scale * maxSpeed
                normalised = scale < 1.0
            }

            val accel = drivetrain.accelMps2
            val brakeRate = drivetrain.brakeMps2
            leftSpeedMps = if (accel != null && brakeRate != null) {
                slew(leftSpeedMps, leftTarget, accel, brakeRate, dtSeconds)
            } else {
                leftTarget
            }
            rightSpeedMps = if (accel != null && brakeRate != null) {
                slew(rightSpeedMps, rightTarget, accel, brakeRate, dtSeconds)
            } else {
                rightTarget
            }

            if (abs(leftSpeedMps) < 1.0e-4 && abs(rightSpeedMps) < 1.0e-4) {
                return coast(actualLeftMps, actualRightMps)
            }

            return Demand(
                state = DriveState.DRIVE,
                left = (leftSpeedMps / maxSpeed).coerceIn(-1.0, 1.0),
                right = (rightSpeedMps / maxSpeed).coerceIn(-1.0, 1.0),
                targetSpeedMps = max(abs(leftSpeedMps), abs(rightSpeedMps)),
                leftTargetSpeedMps = leftSpeedMps,
                rightTargetSpeedMps = rightSpeedMps,
                speedLimited = normalised,
                normalised = normalised,
            )
        }

        private fun coast(actualLeftMps: Double, actualRightMps: Double): Demand {
            if (actualLeftMps.isFinite()) leftSpeedMps = actualLeftMps
            if (actualRightMps.isFinite()) rightSpeedMps = actualRightMps
            return Demand.COAST
        }
    }

    /**
     * One substep's control output.
     *
     * [left]/[right] are normalised demands in `-1..1`; positive is forward.
     * [targetSpeedMps] is the surface speed the side is asked to reach, already
     * limited by the authored maximum. It is a **target**, not a velocity write.
     */
    data class Demand(
        val state: DriveState,
        val left: Double,
        val right: Double,
        val targetSpeedMps: Double,
        /** Surface speed already scaled by [left]. Do not multiply by [left] again. */
        val leftTargetSpeedMps: Double = 0.0,
        /** Surface speed already scaled by [right]. Do not multiply by [right] again. */
        val rightTargetSpeedMps: Double = 0.0,
        /** True when the request exceeded the authored speed cap. */
        val speedLimited: Boolean = false,
        /** True when the per-side normalisation changed the split. */
        val normalised: Boolean = false,
    ) {
        companion object {
            val COAST = Demand(DriveState.COAST, 0.0, 0.0, 0.0)
        }
    }

    /**
     * Resolve one substep of control.
     *
     * [throttle] is `-1..1`, [steer] is `-1..1` (positive turns right),
     * [differential] is the authored skid-steer split and [maxSpeedMps] the
     * authored top speed. Every non-finite input degrades to coast rather than
     * producing a NaN demand that a later stage would have to filter.
     */
    fun resolve(
        throttle: Float,
        steer: Float,
        brake: Boolean,
        powered: Boolean,
        differential: Double,
        maxSpeedMps: Double,
    ): Demand {
        if (!throttle.isFinite() || !steer.isFinite() ||
            !differential.isFinite() || !maxSpeedMps.isFinite() || maxSpeedMps <= 0.0
        ) {
            return Demand.COAST
        }
        if (!powered) return Demand.COAST

        if (brake) {
            // Braking asks for zero surface speed. The sign of the current speed
            // belongs to the contact layer, which alone knows which way the belt
            // is actually moving; guessing it here would drive downhill on a
            // slope when the hull happens to roll backwards.
            return Demand(DriveState.BRAKE, 0.0, 0.0, 0.0)
        }

        val center = throttle.coerceIn(-1f, 1f).toDouble()
        val turn = (steer.coerceIn(-1f, 1f) * differential).coerceIn(-1.5, 1.5)
        var left = center + turn
        var right = center - turn
        val peak = max(abs(left), abs(right))
        val normalised = peak > 1.0
        if (normalised) {
            left /= peak
            right /= peak
        }

        if (abs(left) < 1.0e-4 && abs(right) < 1.0e-4) return Demand.COAST

        // [left] and [right] are already inside -1..1. Multiplying by max speed
        // here is the only scaling; the contact layer must not multiply again.
        val leftSpeed = left * maxSpeedMps
        val rightSpeed = right * maxSpeedMps
        return Demand(
            state = DriveState.DRIVE,
            left = left,
            right = right,
            targetSpeedMps = max(abs(leftSpeed), abs(rightSpeed)),
            leftTargetSpeedMps = leftSpeed,
            rightTargetSpeedMps = rightSpeed,
            speedLimited = normalised,
            normalised = normalised,
        )
    }
}
