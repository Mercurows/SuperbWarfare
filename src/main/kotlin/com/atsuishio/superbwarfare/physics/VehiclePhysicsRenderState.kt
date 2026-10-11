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
    val grounded: Boolean = false,
) {
    fun finite(): Boolean = listOf(leftTravel, rightTravel, leftForce, rightForce,
        leftTrackPhase, rightTrackPhase).all(Float::isFinite)
}
