package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.NativePhysicsLoader
import cn.solarmoon.spark_core.physics.body.collideWithOwnerGroups
import cn.solarmoon.spark_core.physics.body.name
import cn.solarmoon.spark_core.physics.body.owner
import com.jme3.bullet.CollisionConfiguration
import com.jme3.bullet.PhysicsSoftSpace
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.PhysicsTickListener
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Static acceptance for the 99A mechanism in a bare Bullet space.
 *
 * Three independent cases, in the order the load path is built:
 * A. wheels + springs          -> does the spring carry the hull?
 * B. closed belt on the ground -> does the belt stay closed under load?
 * C. belt filtering            -> are shoe-shoe contacts off while wheel-shoe is on?
 */
class TrackedRigStaticTest {

    private val resources = File("src/main/resources")

    private fun spec(): TrackedVehicleSpec {
        val definition = Json { ignoreUnknownKeys = true }.decodeFromString(
            TrackedVehicleDefinition.serializer(),
            File(resources, "data/superbwarfare/sbw/tracked/ztz_99a.json").readText(),
        )
        return TrackedVehicleCompiler.compile(
            "superbwarfare:ztz_99a",
            definition,
            File(resources, "assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json").readText(),
        )
    }

    private fun space(): PhysicsSpace = object : PhysicsSoftSpace(
        Vector3f(-256f, -256f, -256f), Vector3f(256f, 256f, 256f),
        BroadphaseType.DBVT, CollisionConfiguration(8192, 0),
    ) {
        override fun needsCollision(a: PhysicsCollisionObject, b: PhysicsCollisionObject): Boolean =
            TrackedRig.collisionAllowed(
                a.collisionGroup, a.collideWithGroups, a.collideWithOwnerGroups,
                b.collisionGroup, b.collideWithGroups, b.collideWithOwnerGroups,
                a.owner != null && a.owner === b.owner,
            )
    }

    private fun floor(space: PhysicsSpace, top: Float, group: Int) {
        val body = PhysicsRigidBody(BoxCollisionShape(Vector3f(30f, .5f, 30f)).apply { margin = .001f }, 0f)
        body.setPhysicsLocation(Vector3f(0f, top - .5f, 0f))
        body.collisionGroup = group
        body.setCollideWithGroups(0)
        space.addCollisionObject(body)
    }

    private fun chassis(space: PhysicsSpace, spec: TrackedVehicleSpec, y: Float): PhysicsRigidBody =
        PhysicsRigidBody(
            BoxCollisionShape(Vector3f(1.125f, .30f, 2.203f)).apply { margin = .001f },
            spec.sprungMassKg.toFloat(),
        ).apply {
            collisionGroup = TrackedRig.COLLISION_CHASSIS
            // The hull must not collide with its own belt; in game VehicleBody's mask
            // is TERRAIN|CHASSIS|PHYSICS_BODY|WHEEL, i.e. it never includes LINK.
            setCollideWithGroups(TrackedRig.COLLISION_TERRAIN or TrackedRig.COLLISION_WHEEL)
            // Same-owner contacts with its own running gear must not fight the axle joints.
            collideWithOwnerGroups = TrackedRig.COLLISION_NONE
            setPhysicsLocation(Vector3f(0f, y, 0f))
            setProtectGravity(true)
            setGravity(Vector3f())
            friction = .1f
            space.addCollisionObject(this)
        }

    /**
     * Applies one substep of forces and integrates exactly once. Forces are
     * pushed explicitly instead of through a tick listener: a bare Bullet space
     * does not reliably deliver pre-tick callbacks, and a silent no-op there is
     * indistinguishable from a physics failure.
     */
    private fun step(space: PhysicsSpace, rig: TrackedRig, chassis: PhysicsRigidBody, steps: Int) {
        val dt = 1f / 60f
        repeat(steps) {
            chassis.applyCentralForce(Vector3f(0f, -chassis.mass * 9.81f, 0f))
            rig.preStep(dt)
            space.update(dt, 0, false, true, false)
        }
    }

    @Test
    fun a_wheelsAndSpringsCarryTheHullAtTheAuthoredPose() {
        NativePhysicsLoader.load()
        val spec = spec()
        val space = space()
        val wheelBottom = spec.sides.values.flatMap { it.wheels }.minOf { it.center.y - it.radius }
        val y0 = (-wheelBottom).toFloat()
        floor(space, 0f, TrackedRig.COLLISION_TERRAIN)
        val chassis = chassis(space, spec, y0)
        val rig = TrackedRig(space, chassis, spec)
        rig.create(includeLinks = false)
        step(space, rig, chassis, 1)
        val w0 = rig.wheels.first { it.wheel.suspension.isSprung }
        println("DBG m=${chassis.mass} stations=${spec.stationMassTotal} first: travel=${w0.travel} F=${w0.springForceN} expect=${w0.wheel.suspension.stationMassKg * 9.81} static=${w0.wheel.suspension.staticCompressionM}")
        step(space, rig, chassis, 899)

        val y = chassis.getPhysicsLocation(null).y
        val velocity = chassis.getLinearVelocity(null).length()
        val travels = rig.wheels.filter { it.wheel.suspension.isSprung }.map { it.travel }
        val worstTravel = travels.maxOf { abs(it) }
        val forceError = rig.wheels.filter { it.wheel.suspension.isSprung }.maxOf { wheel ->
            val expected = wheel.wheel.suspension.stationMassKg * 9.81
            abs(wheel.springForceN - expected) / expected
        }
        val vv = chassis.getLinearVelocity(null)
        println("A y=$y dy=${y - y0} v=$velocity vx=${vv.x} vy=${vv.y} vz=${vv.z} worstTravel=$worstTravel worstForceError=$forceError")

        assertTrue(y.isFinite(), "chassis pose not finite")
        assertTrue(abs(y - y0) < .02f, "hull did not rest at the authored pose: dy=${y - y0}")
        assertTrue(velocity < .05f, "hull did not settle: v=$velocity")
        assertTrue(worstTravel < .01, "wheel travel not near bind: $worstTravel")
        assertTrue(forceError < .15, "spring force not near static load: $forceError")
    }

    @Test
    fun b_closedBeltStaysClosedUnderLoad() {
        NativePhysicsLoader.load()
        val spec = spec()
        val space = space()
        val wheelBottom = spec.sides.values.flatMap { it.wheels }.minOf { it.center.y - it.radius }
        val thickness = spec.sides.values.first().linkTemplate.thickness
        val y0 = (-wheelBottom + thickness).toFloat()
        floor(space, 0f, TrackedRig.COLLISION_TERRAIN)
        val chassis = chassis(space, spec, y0)
        val rig = TrackedRig(space, chassis, spec)
        rig.create()
        assertEquals(spec.totalLinkCount, rig.links.size, "shoe count")
        step(space, rig, chassis, 1)
        val w0 = rig.wheels.first { it.wheel.suspension.isSprung }
        println("DBG m=${chassis.mass} stations=${spec.stationMassTotal} first: travel=${w0.travel} F=${w0.springForceN} expect=${w0.wheel.suspension.stationMassKg * 9.81} static=${w0.wheel.suspension.staticCompressionM}")
        step(space, rig, chassis, 899)

        val y = chassis.getPhysicsLocation(null).y
        var worstPitch = 0.0
        var finite = true
        for (side in TrackedSide.entries) {
            val sideLinks = rig.links.filter { it.body.name.contains(side.name.lowercase()) }
            for (index in sideLinks.indices) {
                val a = sideLinks[index].body.getPhysicsLocation(null)
                val b = sideLinks[(index + 1) % sideLinks.size].body.getPhysicsLocation(null)
                if (!a.x.isFinite() || !b.x.isFinite()) finite = false
                val distance = sqrt((a.x - b.x).let { it * it } + (a.y - b.y) * (a.y - b.y) + (a.z - b.z) * (a.z - b.z))
                worstPitch = maxOf(worstPitch, abs(distance - spec.pitch))
            }
        }
        val lowest = rig.links.minOf { it.body.getPhysicsLocation(null).y }
        val wheelLow = rig.wheels.minOf { it.body.getPhysicsLocation(null).y - it.wheel.radius }
        println("B y=$y dy=${y - y0} worstPitchError=$worstPitch lowestLinkY=$lowest lowestWheelBottom=$wheelLow")

        assertTrue(finite, "belt pose became non-finite")
        assertTrue(abs(y - y0) < .05f, "belt did not carry the hull: dy=${y - y0}")
        assertTrue(worstPitch < .01, "belt opened: worst pitch error $worstPitch")
    }

    @Test
    fun c_sameOwnerFilterKeepsWheelShoeAndDropsShoeShoe() {
        val wheelGroup = TrackedRig.COLLISION_WHEEL
        val wheelMask = TrackedRig.COLLISION_TERRAIN or TrackedRig.COLLISION_LINK
        val wheelOwner = TrackedRig.COLLISION_LINK
        val linkGroup = TrackedRig.COLLISION_LINK
        val linkMask = TrackedRig.COLLISION_TERRAIN or TrackedRig.COLLISION_WHEEL
        val linkOwner = TrackedRig.COLLISION_NONE

        assertTrue(
            TrackedRig.collisionAllowed(
                wheelGroup, wheelMask, wheelOwner,
                linkGroup, linkMask, linkOwner, sameOwner = true,
            ),
            "a wheel must collide with its own shoes",
        )
        assertFalse(
            TrackedRig.collisionAllowed(
                linkGroup, linkMask, linkOwner,
                linkGroup, linkMask, linkOwner, sameOwner = true,
            ),
            "neighbouring shoes must not collide",
        )
        assertTrue(
            TrackedRig.collisionAllowed(
                linkGroup, linkMask, linkOwner,
                TrackedRig.COLLISION_TERRAIN, 0, 0, sameOwner = false,
            ),
            "a shoe must collide with terrain",
        )
    }
}
