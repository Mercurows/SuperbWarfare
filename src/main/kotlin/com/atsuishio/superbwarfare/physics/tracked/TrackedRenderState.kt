package com.atsuishio.superbwarfare.physics.tracked

import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelSide
import com.atsuishio.superbwarfare.physics.WheelRenderState
import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Immutable wheel-group snapshot shared by the visible plate sampler.
 *
 * It is not a Bullet pose and not a closed shoe chain. The client rebuilds the
 * visible envelope from these wheel centres; it must not write the result back.
 */
@Serializable
data class TrackedWheelRenderPose(
    val index: Int = 0,
    val bone: String = "",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val radius: Double = 0.0,
    val outside: Boolean = true,
    /** True only for a sprung road wheel. Idler and drive wheels are not suspended. */
    val suspended: Boolean = false,
    val suspension: Double = 0.0,
    val tension: Double = 0.0,
    /** Continuous wheel rotation, radians. Not derived from the wrapped belt phase. */
    val spin: Double = 0.0,
) {
    fun finite(): Boolean = listOf(x, y, z, radius, suspension, tension, spin).all(Double::isFinite) &&
        (!suspended || suspension.isFinite())
}

@Serializable
data class TrackedSideRenderState(
    val phase: Double = 0.0,
    val beltSpeed: Double = 0.0,
    val planeX: Double = 0.0,
    val visibleCount: Int = 0,
    val restPitch: Double = 0.0,
    val templateBone: String = "",
    val thickness: Double = 0.0,
    val seatAlong: Double = 0.0,
    val seatNormal: Double = 0.0,
    val seatAngle: Double = 0.0,
    val wheels: List<TrackedWheelRenderPose> = emptyList(),
) {
    fun finite(): Boolean = phase.isFinite() && beltSpeed.isFinite() && planeX.isFinite() &&
        restPitch.isFinite() && thickness.isFinite() && seatAlong.isFinite() &&
        seatNormal.isFinite() && seatAngle.isFinite() && visibleCount >= 0 && wheels.all { it.finite() }
}

@Serializable
data class TrackedRenderState(
    val frame: Long = 0L,
    val originX: Double = 0.0,
    val originY: Double = 0.0,
    val originZ: Double = 0.0,
    val renderScale: Double = 1.0,
    val left: TrackedSideRenderState = TrackedSideRenderState(),
    val right: TrackedSideRenderState = TrackedSideRenderState(),
) {
    fun finite(): Boolean = listOf(originX, originY, originZ, renderScale).all(Double::isFinite) &&
        renderScale > 0.0 && left.finite() && right.finite()

    fun side(side: TrackedSide): TrackedSideRenderState = when (side) {
        TrackedSide.LEFT -> left
        TrackedSide.RIGHT -> right
    }

    fun interpolate(target: TrackedRenderState, alpha: Float): TrackedRenderState {
        val t = alpha.toDouble().coerceIn(0.0, 1.0)
        return copy(
            frame = target.frame,
            originX = lerp(originX, target.originX, t),
            originY = lerp(originY, target.originY, t),
            originZ = lerp(originZ, target.originZ, t),
            renderScale = target.renderScale,
            left = left.interpolate(target.left, t),
            right = right.interpolate(target.right, t),
        )
    }

    /** Compatibility projection for renderers that still read per-wheel travel. */
    fun wheelStates(): List<WheelRenderState> = listOf(TrackedSide.LEFT to left, TrackedSide.RIGHT to right)
        .flatMap { (side, state) ->
            val wheelSide = if (side == TrackedSide.LEFT) WheelSide.LEFT else WheelSide.RIGHT
            state.wheels.map { wheel ->
                WheelRenderState(
                    index = wheel.index,
                    side = wheelSide,
                    suspensionTravel = wheel.suspension.toFloat(),
                    spin = wheel.spin.toFloat(),
                )
            }
        }

    private fun TrackedSideRenderState.interpolate(target: TrackedSideRenderState, alpha: Double): TrackedSideRenderState {
        if (target.wheels.size != wheels.size || target.visibleCount != visibleCount) return target
        val previous = wheels.associateBy { it.index }
        return target.copy(
            phase = wrapLerp(phase, target.phase, alpha, target.visibleCount.toDouble()),
            beltSpeed = lerp(beltSpeed, target.beltSpeed, alpha),
            planeX = lerp(planeX, target.planeX, alpha),
            wheels = target.wheels.map { next ->
                val old = previous[next.index] ?: return@map next
                next.copy(
                    x = lerp(old.x, next.x, alpha),
                    y = lerp(old.y, next.y, alpha),
                    z = lerp(old.z, next.z, alpha),
                    suspension = lerp(old.suspension, next.suspension, alpha),
                    tension = lerp(old.tension, next.tension, alpha),
                    spin = wrapLerp(old.spin, next.spin, alpha, Math.PI * 2.0),
                )
            },
        )
    }

    private fun lerp(from: Double, to: Double, alpha: Double): Double = from + (to - from) * alpha

    private fun wrapLerp(from: Double, to: Double, alpha: Double, cycle: Double): Double {
        if (cycle <= 1.0e-6) return to
        var delta = (to - from) % cycle
        if (delta > cycle * 0.5) delta -= cycle
        if (delta < -cycle * 0.5) delta += cycle
        var value = from + delta * alpha
        value %= cycle
        if (value < 0.0) value += cycle
        return if (abs(value - cycle) < 1.0e-5) 0.0 else value
    }
}
