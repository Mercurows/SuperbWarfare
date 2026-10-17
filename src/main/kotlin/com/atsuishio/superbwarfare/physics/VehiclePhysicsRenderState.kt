package com.atsuishio.superbwarfare.physics

/** Immutable render-side projection of Spark's chassis support state. */
data class VehiclePhysicsRenderState(
    val physicsFrame: Long = 0L,
    val leftTravel: Float = 0f,
    val rightTravel: Float = 0f,
    val leftForce: Float = 0f,
    val rightForce: Float = 0f,
    /** Track animation phase in the vehicle's existing model-space units. */
    val leftTrackPhase: Float = 0f,
    val rightTrackPhase: Float = 0f,
    /** WHEEL independent projections; TRACK wheels share their side's height. */
    val wheels: List<WheelRenderState> = emptyList(),
    val grounded: Boolean = false,
) {
    fun finite(): Boolean = listOf(leftTravel, rightTravel, leftForce, rightForce,
        leftTrackPhase, rightTrackPhase).all(Float::isFinite) && wheels.all { it.finite() }

    /** Interpolates immutable server projections on the client render thread. */
    fun interpolate(target: VehiclePhysicsRenderState, alpha: Float, trackCycle: Float = 100f): VehiclePhysicsRenderState {
        val t = alpha.coerceIn(0f, 1f)
        val previous = wheels.associateBy { it.index to it.side }
        val next = target.wheels.associateBy { it.index to it.side }
        val interpolated = next.map { (key, value) ->
            val old = previous[key] ?: value
            WheelRenderState(
                index = value.index,
                side = value.side,
                suspensionTravel = old.suspensionTravel + (value.suspensionTravel - old.suspensionTravel) * t,
                steeringAngle = old.steeringAngle + (value.steeringAngle - old.steeringAngle) * t,
                spin = interpolateAngle(old.spin, value.spin, t),
                grounded = if (t < .5f) old.grounded else value.grounded,
                steerable = value.steerable,
            )
        }
        return copy(
            physicsFrame = target.physicsFrame,
            leftTravel = leftTravel + (target.leftTravel - leftTravel) * t,
            rightTravel = rightTravel + (target.rightTravel - rightTravel) * t,
            leftForce = leftForce + (target.leftForce - leftForce) * t,
            rightForce = rightForce + (target.rightForce - rightForce) * t,
            leftTrackPhase = interpolatePhase(leftTrackPhase, target.leftTrackPhase, t, trackCycle),
            rightTrackPhase = interpolatePhase(rightTrackPhase, target.rightTrackPhase, t, trackCycle),
            wheels = interpolated,
            grounded = if (t < .5f) grounded else target.grounded,
        )
    }

    companion object {
        private fun interpolateAngle(from: Float, to: Float, alpha: Float): Float {
            val delta = ((to - from + Math.PI.toFloat()) % (Math.PI.toFloat() * 2f) +
                Math.PI.toFloat() * 2f) % (Math.PI.toFloat() * 2f) - Math.PI.toFloat()
            return from + delta * alpha
        }

        private fun interpolatePhase(from: Float, to: Float, alpha: Float, cycle: Float): Float =
            from + (((to - from + cycle * .5f) % cycle + cycle) % cycle - cycle * .5f) * alpha
    }
}
