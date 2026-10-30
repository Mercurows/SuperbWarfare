package com.atsuishio.superbwarfare.physics.tracked

import com.jme3.math.Matrix3f
import com.jme3.math.Vector3f

/**
 * Chassis axes shared by drive, traction measurement and visible track phase.
 *
 * Bedrock authors the gun toward model `-Z`. [BedrockModelGeometry.toPhysicsLocal]
 * flips that axis, so the nose is chassis `+Z`. The entity look vector and the
 * renderer's `180 - yaw` turn agree with that nose. Drive used to treat `-Z` as
 * forward, which sent the hull toward the visual rear.
 */
internal object TrackedVehicleFrame {
    val localForward: Vector3f = Vector3f(0f, 0f, 1f)

    fun worldForward(rotation: Matrix3f): Vector3f {
        val forward = rotation.mult(Vector3f(localForward), Vector3f())
        return if (forward.lengthSquared() < 1.0e-8f) Vector3f() else forward.normalize()
    }
}
