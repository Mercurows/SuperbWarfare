package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.NativePhysicsLoader
import cn.solarmoon.spark_core.physics.body.PhysicsJoints
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CylinderCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import org.junit.jupiter.api.Test

class SixDofSpringProbeTest {
    @Test
    fun equilibriumIsSnapped() {
        NativePhysicsLoader.load()
        val space = PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT)
        space.setGravity(Vector3f())
        val floor = PhysicsRigidBody(BoxCollisionShape(Vector3f(20f, .5f, 20f)), 0f)
        floor.setPhysicsLocation(Vector3f(0f, -.5f, 0f))
        floor.collisionGroup = 1
        floor.setCollideWithGroups(0)
        space.addCollisionObject(floor)
        val chassis = PhysicsRigidBody(BoxCollisionShape(Vector3f(.6f, .3f, .6f)), 3_793f)
        val wheel = PhysicsRigidBody(CylinderCollisionShape(Vector3f(.1f, .53f, .53f), 0), 900f)
        for (b in listOf(chassis, wheel)) { b.setProtectGravity(true); b.setGravity(Vector3f()) }
        chassis.collisionGroup = 1 shl 4; chassis.setCollideWithGroups(0)
        wheel.collisionGroup = 1 shl 5; wheel.setCollideWithGroups(1)
        chassis.setPhysicsLocation(Vector3f(0f, .53f, 0f))
        wheel.setPhysicsLocation(Vector3f(0f, .53f, 0f))
        space.addCollisionObject(chassis); space.addCollisionObject(wheel)

        val joint = PhysicsJoints.wheelAxleSpring(
            chassis, wheel, Vector3f(), Vector3f(), -.24f, .18f, 633_356f, 47_000f, .0513f,
        )
        println("PROBE eq=${joint.getEquilibriumPoint(1)} k=${joint.getStiffness(1)} c=${joint.getDamping(1)} " +
            "spring=${joint.isSpringEnabled(1)}")
        space.addJoint(joint)
        repeat(600) {
            chassis.applyCentralForce(Vector3f(0f, -chassis.mass * 9.81f, 0f))
            wheel.applyCentralForce(Vector3f(0f, -wheel.mass * 9.81f, 0f))
            space.update(1f / 60f, 0, false, true, false)
        }
        println("PROBE chassisY=${chassis.getPhysicsLocation(null).y} wheelY=${wheel.getPhysicsLocation(null).y} " +
            "travel=${wheel.getPhysicsLocation(null).y - chassis.getPhysicsLocation(null).y} " +
            "v=${chassis.getLinearVelocity(null).y}")
        space.destroy()
    }
}
