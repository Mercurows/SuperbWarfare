package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.NativePhysicsLoader
import cn.solarmoon.spark_core.physics.body.PhysicsJoints
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CylinderCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Matrix3f
import com.jme3.math.Vector3f
import org.junit.jupiter.api.Test

/**
 * One station of the 99A suspension, in isolation: a hull carrying 3.79 t, one
 * 450 kg road wheel on the floor, one guided axle with a joint spring.
 *
 * Purpose: decide the sign of the spring equilibrium — the full rig currently
 * pushes its wheels up to the bump stop, which is not a stiffness problem.
 */
class SingleStationSpringTest {

    private fun run(equilibrium: Float, label: String) {
        val space = PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT)
        space.setGravity(Vector3f())
        val floor = PhysicsRigidBody(BoxCollisionShape(Vector3f(20f, .5f, 20f)).apply { margin = .001f }, 0f)
        floor.setPhysicsLocation(Vector3f(0f, -.5f, 0f))
        floor.collisionGroup = 1
        floor.setCollideWithGroups(0)
        space.addCollisionObject(floor)

        val chassis = PhysicsRigidBody(BoxCollisionShape(Vector3f(.6f, .3f, .6f)).apply { margin = .001f }, 3_793f)
        val wheel = PhysicsRigidBody(CylinderCollisionShape(Vector3f(.1f, .53f, .53f), 0).apply { margin = .001f }, 450f)
        for (body in listOf(chassis, wheel)) {
            body.setProtectGravity(true)
            body.setGravity(Vector3f())
        }
        chassis.collisionGroup = 1 shl 4
        chassis.setCollideWithGroups(0)
        wheel.collisionGroup = 1 shl 5
        wheel.setCollideWithGroups(1)
        chassis.setPhysicsLocation(Vector3f(0f, .53f, 0f))
        wheel.setPhysicsLocation(Vector3f(0f, .53f, 0f))
        space.addCollisionObject(chassis)
        space.addCollisionObject(wheel)

        val stability = TrackedStability.clamp(724_686.0, 47_432.0, TrackedStability.effectiveMassKg(3793.0, 450.0), 1.0 / 60.0)
        val joint = PhysicsJoints.guidedWheelAxle(
            chassis, wheel, Vector3f(), Vector3f(), Matrix3f(), Matrix3f(), -.24f, .18f,
        )
        joint.enableSpring(1, true)
        joint.setStiffness(1, stability.stiffnessNPerM.toFloat(), true)
        joint.setDamping(1, stability.dampingNsPerM.toFloat(), true)
        joint.setEquilibriumPoint(1, equilibrium)
        space.addJoint(joint)

        repeat(900) {
            chassis.applyCentralForce(Vector3f(0f, -chassis.mass * 9.81f, 0f))
            wheel.applyCentralForce(Vector3f(0f, -wheel.mass * 9.81f, 0f))
            space.update(1f / 60f, 0, false, true, false)
        }
        val chassisY = chassis.getPhysicsLocation(null).y
        val wheelY = wheel.getPhysicsLocation(null).y
        println(
            "STATION $label eq=$equilibrium k=${stability.stiffnessNPerM} c=${stability.dampingNsPerM} " +
                "-> chassisY=$chassisY wheelY=$wheelY travel=${wheelY - chassisY} " +
                "v=${chassis.getLinearVelocity(null).y}",
        )
        space.destroy()
    }

    @Test
    fun singleStationHoldsTheHull() {
        NativePhysicsLoader.load()
        run(-0.0513f, "negative-eq")
        run(0.0513f, "positive-eq")
    }
}
