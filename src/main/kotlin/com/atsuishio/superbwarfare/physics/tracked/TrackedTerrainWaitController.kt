package com.atsuishio.superbwarfare.physics.tracked

import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f

/**
 * Holds a tracked assembly still while its local terrain mesh is being built or
 * replaced, **without** turning any body kinematic.
 *
 * Bullet does not support a joint between two kinematic bodies, and a tracked
 * vehicle already has mounts and run-end constraints between the chassis, the
 * wheel ends and the belt runs. Flipping them kinematic while waiting therefore
 * silently invalidates the mechanism instead of merely pausing it. The reference
 * implementation avoids the same trap for the same reason.
 *
 * The hold is instead a factor lock: the body stays dynamic, its forces are
 * cleared and its linear/angular factors go to zero, so the solver still sees a
 * valid dynamic body with valid joints but cannot move it. Velocities are
 * captured and restored so a short wait does not erase the vehicle's momentum.
 *
 * Physics thread only. Every method asserts that through the caller.
 */
class TrackedTerrainWaitController {

    private class Held(
        val linearFactor: Vector3f,
        val angularFactor: Vector3f,
        val linearVelocity: Vector3f,
        val angularVelocity: Vector3f,
    )

    private val held = LinkedHashMap<PhysicsRigidBody, Held>()

    val size: Int get() = held.size
    val isEmpty: Boolean get() = held.isEmpty()

    /** True while [body] is factor-locked by this controller. */
    fun isHolding(body: PhysicsRigidBody): Boolean = held.containsKey(body)

    /**
     * Lock [body] in place until [release]. Idempotent: a body that is already
     * held keeps its originally captured state, so a second hold cannot record
     * the zeroed factors as "the original".
     */
    fun hold(body: PhysicsRigidBody) {
        if (body.isStatic) return
        if (held.containsKey(body)) return
        // A user-authored kinematic body keeps its own state. We never convert a
        // dynamic body either way, and we never overwrite an authored choice.
        if (body.isKinematic) return
        held[body] = Held(
            body.getLinearFactor(null).clone(),
            body.getAngularFactor(null).clone(),
            body.getLinearVelocity(null).clone(),
            body.getAngularVelocity(null).clone(),
        )
        body.clearForces()
        body.setLinearFactor(Vector3f(0f, 0f, 0f))
        body.setAngularFactor(Vector3f(0f, 0f, 0f))
        body.setLinearVelocity(Vector3f())
        body.setAngularVelocity(Vector3f())
        body.activate()
    }

    /**
     * Restore the factors and, when [restoreVelocity] is set, the captured
     * motion. A mesh *replacement* is a geometry edit under a moving vehicle and
     * must keep its momentum; a mesh *loss* (terrain became unknown) must not
     * hand back motion the vehicle earned on geometry that no longer exists.
     */
    fun release(body: PhysicsRigidBody, restoreVelocity: Boolean = true) {
        val state = held.remove(body) ?: return
        if (!body.isInWorld) return
        body.clearForces()
        body.setLinearFactor(state.linearFactor)
        body.setAngularFactor(state.angularFactor)
        if (restoreVelocity) {
            body.setLinearVelocity(state.linearVelocity)
            body.setAngularVelocity(state.angularVelocity)
        } else {
            body.setLinearVelocity(Vector3f())
            body.setAngularVelocity(Vector3f())
        }
        body.activate()
    }

    fun holdAll(bodies: Collection<PhysicsRigidBody>) {
        for (body in bodies) hold(body)
    }

    fun releaseAll(bodies: Collection<PhysicsRigidBody>, restoreVelocity: Boolean = true) {
        for (body in bodies) release(body, restoreVelocity)
    }

    /** Release everything, dropping the captured motion (teardown path). */
    fun clear(dropMotion: Boolean = true) {
        for (body in held.keys.toList()) release(body, restoreVelocity = !dropMotion)
        held.clear()
    }

    /** Bodies currently factor-locked, for diagnostics and tests. */
    fun heldBodies(): Set<PhysicsRigidBody> = held.keys.toSet()
}