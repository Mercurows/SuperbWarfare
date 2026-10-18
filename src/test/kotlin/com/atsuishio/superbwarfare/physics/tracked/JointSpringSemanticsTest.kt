package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.NativePhysicsLoader
import cn.solarmoon.spark_core.physics.body.PhysicsJoints
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Matrix3f
import com.jme3.math.Vector3f
import org.junit.jupiter.api.Test

/**
 * Determines the real semantics of `New6Dof` spring axes as exposed by
 * libbulletjme 22.0.3: is the equilibrium signed along +LIN_Y, and does
 * `limitIfNeeded` disable the spring row or only relax its limit?
 *
 * The tracked rig depends on both answers, so this test probes them explicitly
 * instead of assuming.
 */
class JointSpringSemanticsTest {

    private fun probe(equilibrium: Float, limitIfNeeded: Boolean, label: String) {
        val space = PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT)
        space.setGravity(Vector3f())
        val chassis = PhysicsRigidBody(BoxCollisionShape(Vector3f(1f, .25f, 1f)), 4_000f)
        val wheel = PhysicsRigidBody(BoxCollisionShape(Vector3f(.2f, .2f, .2f)), 450f)
        chassis.setProtectGravity(true); chassis.setGravity(Vector3f())
        wheel.setProtectGravity(true); wheel.setGravity(Vector3f())
        chassis.collisionGroup = 1 shl 4
        wheel.collisionGroup = 1 shl 5
        chassis.setCollideWithGroups(0)
        wheel.setCollideWithGroups(0)
        chassis.setPhysicsLocation(Vector3f(0f, 5f, 0f))
        wheel.setPhysicsLocation(Vector3f(0f, 5f, 0f))
        space.addCollisionObject(chassis)
        space.addCollisionObject(wheel)

        val k = 724_400f
        val c = 47_430f
        val joint = PhysicsJoints.guidedWheelAxle(
            chassis, wheel, Vector3f(), Vector3f(), Matrix3f(), Matrix3f(), -.24f, .18f,
        )
        joint.enableSpring(1, true)
        joint.setStiffness(1, k, limitIfNeeded)
        joint.setDamping(1, c, limitIfNeeded)
        joint.setEquilibriumPoint(1, equilibrium)
        space.addJoint(joint)

        repeat(900) {
            chassis.applyCentralForce(Vector3f(0f, -chassis.mass * 9.81f, 0f))
            wheel.applyCentralForce(Vector3f(0f, -wheel.mass * 9.81f, 0f))
            space.update(1f / 60f, 0, false, true, false)
        }
        val travel = wheel.getPhysicsLocation(null).y - chassis.getPhysicsLocation(null).y
        val velocity = wheel.getLinearVelocity(null).y - chassis.getLinearVelocity(null).y
        println(
            "PROBE $label eq=$equilibrium limitIfNeeded=$limitIfNeeded -> travel=$travel v=$velocity " +
                "chassisY=${chassis.getPhysicsLocation(null).y} wheelY=${wheel.getPhysicsLocation(null).y}",
        )
        space.destroy()
    }

    @Test
    fun probeSpringAxisSemantics() {
        NativePhysicsLoader.load()
        probe(-0.0513f, false, "negative-eq/no-limit")
        probe(0.0513f, false, "positive-eq/no-limit")
        probe(-0.0513f, true, "negative-eq/limit")
        probe(0.0513f, true, "positive-eq/limit")
    }
}
