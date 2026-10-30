package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.NativePhysicsLoader
import cn.solarmoon.spark_core.physics.PhysicsSuspensionRow
import cn.solarmoon.spark_core.physics.SuspensionRowState
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

/**
 * B1 re-review: the belt-member mount as it is actually compiled.
 *
 * The earlier B1 fixture used `guidedWheelAxle()`, which frees the wheel's axle
 * rotation. That is the right primitive for a freely rolling wheel, but it is
 * **not** the mount a belt member gets: a belt member is mounted without an
 * independent spin degree of freedom. This fixture therefore consumes
 * `CompiledWheelMount` and `TrackedJointFactory`, the same pair the vehicle
 * runtime will use, so a wrong travel axis or a leaked rotation fails here.
 *
 * It also re-checks the bilateral spring law against the new mount, because the
 * force row and the joint are two halves of the same station.
 */
class TrackedWheelMountTest {

    private val resources = File("src/main/resources")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        NativePhysicsLoader.load()
    }

    private fun spec(): TrackedVehicleSpec = TrackedVehicleCompiler.compile(
        "superbwarfare:ztz_99a",
        json.decodeFromString(
            TrackedVehicleDefinition.serializer(),
            File(resources, "data/superbwarfare/sbw/tracked/ztz_99a.json").readText(),
        ),
        File(resources, "assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json").readText(),
    ).requireSpec()

    private fun space(gravity: Vector3f = Vector3f()): PhysicsSpace =
        PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT).apply { setGravity(gravity) }

    private fun body(space: PhysicsSpace, mass: Float): PhysicsRigidBody {
        val body = PhysicsRigidBody(BoxCollisionShape(0.1f), mass)
        body.isContactResponse = false
        space.add(body)
        return body
    }

    /**
     * Every compiled mount must build, and its six axis states must survive the
     * round trip through the native joint.
     *
     * This is what "the plan is executable" means: not that the data class holds
     * the right enum, but that the solver reads back the same limits.
     */
    @Test
    fun everyCompiledMountBuildsAndKeepsItsAxisStates() {
        val spec = spec()
        val space = space()
        try {
            for (side in TrackedSide.entries) {
                val compiled = spec.side(side)
                for (wheel in compiled.wheels) {
                    val mount = compiled.mount(wheel.index)
                    val plan = mount.joint
                    assertNotNull(plan, "${wheel.bone} has no joint plan")
                    val chassis = body(space, 0f)
                    val wheelBody = body(space, 1f)
                    val joint = TrackedJointFactory.create(plan!!, chassis, wheelBody)
                    try {
                        for (axis in 0..2) {
                            assertLimitMatches(joint, axis, plan.linear[axis], "${wheel.bone} linear $axis")
                        }
                        for (axis in 0..2) {
                            assertLimitMatches(
                                joint,
                                cn.solarmoon.spark_core.physics.body.PhysicsJoints.ANG_X + axis,
                                plan.angular[axis],
                                "${wheel.bone} angular $axis",
                            )
                        }
                    } finally {
                        space.removeJoint(joint)
                    }
                }
            }
        } finally {
            space.destroy()
        }
    }

    private fun assertLimitMatches(
        joint: com.jme3.bullet.joints.New6Dof,
        axis: Int,
        limit: CompiledAxisLimit,
        label: String,
    ) {
        val lower = joint.get(com.jme3.bullet.joints.motors.MotorParam.LowerLimit, axis)
        val upper = joint.get(com.jme3.bullet.joints.motors.MotorParam.UpperLimit, axis)
        when (limit) {
            is CompiledAxisLimit.Free -> assertTrue(lower > upper, "$label must be free, was $lower..$upper")
            is CompiledAxisLimit.Locked -> {
                assertEquals(0f, lower, 0f, "$label lower")
                assertEquals(0f, upper, 0f, "$label upper")
            }

            is CompiledAxisLimit.Limited -> {
                assertEquals(limit.lower.toFloat(), lower, 1e-5f, "$label lower")
                assertEquals(limit.upper.toFloat(), upper, 1e-5f, "$label upper")
            }
        }
    }

    /**
     * A Support mount allows exactly one translation and no rotation.
     *
     * The point is the *absence* of an axle degree of freedom: a belt-member wheel
     * must not spin independently of its mount.
     */
    @Test
    fun aSupportMountMovesOnlyOnItsTravelAxisAndCannotSpin() {
        val spec = spec()
        val space = space(Vector3f(0f, -9.81f, 0f))
        try {
            val support = spec.side(TrackedSide.LEFT).wheels
                .first { it.role == TrackWheelRole.SUPPORT }
            val mount = spec.side(TrackedSide.LEFT).mount(support.index)
            val chassis = body(space, 0f)
            val wheelBody = body(space, support.massKg.toFloat())
            wheelBody.setPhysicsLocation(support.bindCenter.toBullet())
            val joint = TrackedJointFactory.create(mount.joint!!, chassis, wheelBody)
            space.addJoint(joint)
            try {
                val start = wheelBody.getPhysicsLocation(null)
                val dt = 1f / 60f
                repeat(60) { space.update(dt, 0, false, true, false) }
                val end = wheelBody.getPhysicsLocation(null)

                // Gravity pulls along world -Y, which is the travel axis, so the
                // wheel may move; it must not move sideways.
                val sideways = Vector3f(end.x - start.x, 0f, end.z - start.z).length()
                assertTrue(sideways < 0.01f, "travel must stay on its axis, drifted $sideways m")

                // And it must be stopped by the travel window, not fall freely.
                // Without this a missing joint looks exactly like a constrained
                // mount: free fall is also "no sideways drift and no rotation".
                val travelled = abs(end.y - start.y).toDouble()
                val freeFall = 0.5 * 9.81 * 1.0 * 1.0
                assertTrue(
                    travelled < support.suspension.travelDown + 0.02,
                    "travel $travelled m must be inside the window, free fall would be $freeFall m",
                )

                // And it must not rotate: no spin freedom, no flop. Compare the
                // basis vectors instead of decomposing Euler angles, because a
                // degenerate decomposition can mask a real rotation.
                val rotation = wheelBody.getPhysicsRotation(null)
                val matrix = rotation.toRotationMatrix()
                val xAxis = matrix.mult(Vector3f(1f, 0f, 0f), Vector3f())
                val yAxis = matrix.mult(Vector3f(0f, 1f, 0f), Vector3f())
                val deviation = xAxis.subtract(Vector3f(1f, 0f, 0f)).length() +
                    yAxis.subtract(Vector3f(0f, 1f, 0f)).length()
                assertTrue(deviation < 0.02f, "a belt-member mount must not rotate, basis deviation $deviation")
            } finally {
                space.removeJoint(joint)
            }
        } finally {
            space.destroy()
        }
    }

    /**
     * 99A's idler is a fixed guide. Gravity and a longitudinal push must not
     * move it; belt length is not recovered by sliding the guide wheel.
     */
    @Test
    fun aFixedIdlerDoesNotMove() {
        val spec = spec()
        val space = space(Vector3f(0f, -9.81f, 0f))
        try {
            val idler = spec.side(TrackedSide.LEFT).wheels
                .single { it.role == TrackWheelRole.IDLER_GUIDE }
            val mount = spec.side(TrackedSide.LEFT).mount(idler.index)
            assertFalse(mount.isTensioned, "99A idler must not carry a tensioner")
            assertFalse(mount.isSprung, "99A idler must not be suspended")
            assertEquals(WheelMountKind.FIXED, mount.kind)

            val chassis = body(space, 0f)
            val wheelBody = body(space, idler.massKg.toFloat())
            wheelBody.setPhysicsLocation(idler.bindCenter.toBullet())
            val joint = TrackedJointFactory.create(mount.joint!!, chassis, wheelBody)
            space.addJoint(joint)
            try {
                val start = wheelBody.getPhysicsLocation(null)
                val dt = 1f / 60f
                repeat(60) { space.update(dt, 0, false, true, false) }
                wheelBody.applyCentralForce(Vector3f(0f, 0f, 5000f))
                repeat(60) { space.update(dt, 0, false, true, false) }
                val end = wheelBody.getPhysicsLocation(null)

                assertEquals(0.0, abs(end.y - start.y).toDouble(), 0.01, "idler must not sag")
                assertEquals(0.0, abs(end.x - start.x).toDouble(), 0.01, "idler must not move laterally")
                assertEquals(0.0, abs(end.z - start.z).toDouble(), 0.01, "idler must not slide along the belt")
            } finally {
                space.removeJoint(joint)
            }
        } finally {
            space.destroy()
        }
    }

    /** A drive sprocket and a return roller are fully fixed. */
    @Test
    fun fixedRolesHaveNoDegreeOfFreedom() {
        val spec = spec()
        val space = space(Vector3f(0f, -9.81f, 0f))
        try {
            for (side in TrackedSide.entries) {
                val wheel = spec.side(side).wheels.single { it.role == TrackWheelRole.DRIVE_GUIDE }
                val mount = spec.side(side).mount(wheel.index)
                assertEquals(WheelMountKind.FIXED, mount.kind, "${wheel.bone} kind")
                val plan = mount.joint!!
                assertTrue(plan.freeLinearAxes.isEmpty(), "${wheel.bone} must have no translation")
                assertTrue(plan.freeAngularAxes.isEmpty(), "${wheel.bone} must have no rotation")

                val chassis = body(space, 0f)
                val wheelBody = body(space, wheel.massKg.toFloat())
                wheelBody.setPhysicsLocation(wheel.bindCenter.toBullet())
                val joint = TrackedJointFactory.create(plan, chassis, wheelBody)
                space.addJoint(joint)
                try {
                    val start = wheelBody.getPhysicsLocation(null)
                    val dt = 1f / 60f
                    repeat(60) { space.update(dt, 0, false, true, false) }
                    assertEquals(
                        0.0,
                        start.distance(wheelBody.getPhysicsLocation(null)).toDouble(),
                        0.001,
                        "${wheel.bone} must not move under gravity",
                    )
                } finally {
                    space.removeJoint(joint)
                }
            }
        } finally {
            space.destroy()
        }
    }

    /**
     * The bilateral row still produces the signed force the suspension needs.
     *
     * Rechecked here rather than assumed: the mount now comes from
     * `CompiledWheelMount`, so a station whose row was silently dropped would
     * still build a joint and look fine.
     */
    @Test
    fun theRowRemainsBilateralOnACompiledMount() {
        val spec = spec()
        val station = spec.side(TrackedSide.LEFT).suspension.stations.first()
        val row = station.toRowSpec()
        assertTrue(row.isUsable, "compiled row must be usable")

        val compressed = PhysicsSuspensionRow.force(
            row,
            SuspensionRowState(travelM = row.travelUpM * 0.5f, velocityMps = 0f),
        )
        val drooped = PhysicsSuspensionRow.force(
            row,
            SuspensionRowState(travelM = -row.travelDownM * 0.5f, velocityMps = 0f),
        )
        assertTrue(compressed > 0f, "compression must push, was $compressed N")
        assertTrue(drooped < 0f, "droop must pull back, was $drooped N")
        assertTrue(abs(compressed) <= row.maxForceN + 1e-3f, "compression must respect MaxForce")
        assertTrue(abs(drooped) <= row.maxForceN + 1e-3f, "droop must respect MaxForce")
    }

    private fun net.minecraft.world.phys.Vec3.toBullet(): Vector3f =
        Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
}
