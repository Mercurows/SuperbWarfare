package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.NativePhysicsLoader
import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.PhysicsSuspensionRow
import cn.solarmoon.spark_core.physics.SuspensionRowState
import cn.solarmoon.spark_core.physics.body.CollisionGroups
import cn.solarmoon.spark_core.physics.body.PhysicsJoints
import cn.solarmoon.spark_core.physics.body.owner
import cn.solarmoon.spark_core.physics.track.TrackRunAssembly
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CylinderCollisionShape
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

/**
 * B2.4: both sides of one vehicle, a free chassis, an external impulse and
 * teardown.
 *
 * Unlike B2.3 this stage has both belts, so the chassis is **dynamic and free**:
 * the two sides together carry it, and no fixture rail holds it up. That makes
 * the load-path and the roll balance real rather than declared.
 *
 * What is checked:
 *
 * - both sides assemble from the shared adapters, with mirrored belt planes;
 * - the sides are laterally disjoint and no joint spans them;
 * - the runs, not the wheels or the chassis, reach the terrain on both sides;
 * - 600 substeps of settling followed by a known impulse move the chassis and
 *   leave both sides' anchors intact;
 * - teardown removes joints before bodies and is idempotent.
 *
 * Still not claimed: real Minecraft `VoxelShape` terrain (S3), drive (S4), and
 * the `TrackRunRuntime` lifecycle (covered by the runtime GameTest).
 *
 * No threshold is frozen; the assertions are structural and the measured values
 * are printed for later confirmation.
 */
class TrackedBothSidesRunNativeTest {

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

    private class Vehicle(
        val space: PhysicsSpace,
        val chassis: PhysicsRigidBody,
        val views: Map<TrackedSide, CompiledTrackSide>,
        val wheels: Map<TrackedSide, List<PhysicsRigidBody>>,
        val runs: Map<TrackedSide, List<PhysicsRigidBody>>,
        val joints: List<New6Dof>,
        val rows: Map<TrackedSide, Map<Int, cn.solarmoon.spark_core.physics.SuspensionRowSpec>>,
        val host: PhysicsHost,
        val tally: TrackedMechanismFixture.ContactTally,
    ) : AutoCloseable {
        override fun close() {
            // Joints first, then the space: a joint must never outlive a body.
            joints.forEach { space.removeJoint(it) }
            space.destroy()
        }

        fun runBody(side: TrackedSide, index: Int): PhysicsRigidBody =
            runs.getValue(side)[views.getValue(side).runs.indexOfFirst { it.index == index }]

        fun wheelBody(side: TrackedSide, wheelIndex: Int): PhysicsRigidBody =
            wheels.getValue(side)[views.getValue(side).wheels.indexOfFirst { it.index == wheelIndex }]

        fun firstEndAnchorGap(side: TrackedSide): Double {
            val view = views.getValue(side)
            var worst = 0.0
            for (run in view.runs) {
                val plan = run.first.joint
                val runBody = runBody(side, run.index)
                val wheel = wheelBody(side, plan.bodyA.ordinal)
                val a = worldPoint(runBody, rotate(plan.frameInB.origin, plan.frameInB.rotation))
                val b = worldPoint(wheel, rotate(plan.frameInA.origin, plan.frameInA.rotation))
                worst = maxOf(worst, a.subtract(b).length().toDouble())
            }
            return worst
        }

        fun travelOf(side: TrackedSide, wheel: CompiledWheel): Float {
            require(wheel.role == TrackWheelRole.SUPPORT) {
                "${wheel.bone} is ${wheel.role}; only a sprung station has a travel coordinate"
            }
            val body = wheelBody(side, wheel.index)
            val plan = views.getValue(side).mount(wheel.index).joint ?: return 0f
            val chassisInverse = chassis.getPhysicsRotation(null).inverse().toRotationMatrix()
            val relative = body.getPhysicsLocation(null).subtract(chassis.getPhysicsLocation(null))
            val inChassisFrame = chassisInverse.mult(relative, Vector3f())
            val anchor = rotate(plan.frameInA.origin, plan.frameInA.rotation)
            val frameInverse = quaternionOf(plan.frameInA.rotation).inverse().toRotationMatrix()
            return frameInverse.mult(
                Vector3f(anchor.x.toFloat(), anchor.y.toFloat(), anchor.z.toFloat()).let {
                    inChassisFrame.subtract(it)
                },
                Vector3f(),
            ).y
        }

        fun rowAxis(side: TrackedSide, wheel: CompiledWheel): Vector3f {
            val plan = views.getValue(side).mount(wheel.index).joint ?: return Vector3f(0f, 1f, 0f)
            val local = quaternionOf(plan.frameInA.rotation).toRotationMatrix()
                .mult(Vector3f(0f, 1f, 0f), Vector3f())
            return chassis.getPhysicsRotation(null).toRotationMatrix().mult(local, Vector3f()).normalize()
        }

        fun applySuspensionForces() {
            for (side in TrackedSide.entries) {
                val view = views.getValue(side)
                for (wheel in view.wheels) {
                    val row = rows.getValue(side)[wheel.index] ?: continue
                    val body = wheelBody(side, wheel.index)
                    val velocity = body.getLinearVelocity(null)
                        .subtract(chassis.getLinearVelocity(null))
                        .dot(rowAxis(side, wheel))
                    val force = PhysicsSuspensionRow.force(
                        row,
                        SuspensionRowState(travelOf(side, wheel), velocity),
                    )
                    PhysicsJoints.applyPairedForce(
                        chassis,
                        body,
                        rowAxis(side, wheel),
                        force,
                        body.getPhysicsLocation(null),
                    )
                }
            }
        }

        fun step(substeps: Int) {
            repeat(substeps) {
                applySuspensionForces()
                space.update(1f / 60f, 0, false, true, false)
                tally.sample(space, allWheels(), allRuns(), chassis, sideGroups())
            }
        }

        private fun allWheels() = TrackedSide.entries.flatMap { wheels.getValue(it) }

        private fun allRuns() = TrackedSide.entries.flatMap { runs.getValue(it) }

        /**
         * Tag every run and wheel body with its side, so the window counters can
         * be split per side. A merged total would let one side carry the vehicle
         * while the other only hangs on its mounts.
         */
        private fun sideGroups(): Map<PhysicsRigidBody, String> {
            val map = HashMap<PhysicsRigidBody, String>()
            for (side in TrackedSide.entries) {
                wheels.getValue(side).forEach { map[it] = side.name }
                runs.getValue(side).forEach { map[it] = side.name }
            }
            return map
        }
    }

    private fun buildVehicle(spec: TrackedVehicleSpec, dropMountJoints: Boolean = false): Vehicle {
        val space = PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT)
        space.setGravity(Vector3f(0f, -9.81f, 0f))
        val host = object : PhysicsHost {
            override val physicsLevel: cn.solarmoon.spark_core.physics.level.PhysicsLevel
                get() = throw UnsupportedOperationException("native fixture has no PhysicsLevel")
            override val allPhysicsBodies: MutableMap<String, PhysicsCollisionObject> =
                java.util.concurrent.ConcurrentHashMap()
        }
        val tally = TrackedMechanismFixture.ContactTally()

        // Flat, static floor: the AABB-equivalent stand-in. Minecraft VoxelShape
        // terrain is S3 and is not claimed by this fixture.
        val floor = PhysicsRigidBody(BoxCollisionShape(60f, 0.5f, 60f), 0f)
        floor.setPhysicsLocation(Vector3f(0f, -8f, 0f))
        floor.friction = .9f
        floor.collisionGroup = TrackedTerrainCollision.TERRAIN_MESH
        floor.setCollideWithGroups(CollisionGroups.NONE)
        space.addCollisionObject(floor)

        // A free chassis carrying the whole sprung assembly. Both sides support
        // it, so no fixture rail is needed or present.
        // Diagnostic hull proxy, kept clear of both belts at bind so a
        // run-chassis contact means the mechanism actually collapsed rather than
        // the proxy being drawn through the belt.
        val chassis = PhysicsRigidBody(BoxCollisionShape(1.3f, 0.25f, 2.0f), spec.sprungAssemblyMassKg.toFloat())
        chassis.isContactResponse = true
        chassis.setPhysicsLocation(Vector3f(0f, -6.3f, 0f))
        chassis.owner = host
        chassis.collisionGroup = CollisionGroups.CHASSIS
        // Terrain stays enabled so a chassis-on-ground bypass would be visible.
        // Track links are not collidable with the chassis: the chassis rides the
        // springs, not the belt.
        chassis.setCollideWithGroups(TrackedTerrainCollision.TERRAIN_MESH)
        chassis.setSleepingThresholds(0f, 0f)
        space.addCollisionObject(chassis)

        val views = LinkedHashMap<TrackedSide, CompiledTrackSide>()
        val wheels = LinkedHashMap<TrackedSide, List<PhysicsRigidBody>>()
        val runs = LinkedHashMap<TrackedSide, List<PhysicsRigidBody>>()
        val rows = LinkedHashMap<TrackedSide, Map<Int, cn.solarmoon.spark_core.physics.SuspensionRowSpec>>()
        val joints = ArrayList<New6Dof>()

        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val members = compiled.members
            val byIndex = compiled.wheels.associateBy { it.index }
            val masses = TrackedMassAllocator.allocateRuns(members, byIndex, compiled.runMassTotalKg)
            val sideRuns = TrackedRunGeometry.compile(members, byIndex, compiled.belt, masses)
            val view = CompiledTrackSide(
                side = side,
                wheels = compiled.wheels,
                belt = compiled.belt,
                beltOrder = compiled.beltOrder,
                members = members,
                runs = sideRuns,
                mounts = compiled.mounts,
                suspension = compiled.suspension,
                legacy = null,
            )
            views[side] = view

            val wheelBodies = ArrayList<PhysicsRigidBody>(view.wheels.size)
            val wheelByIndex = HashMap<Int, PhysicsRigidBody>()
            val sideRows = HashMap<Int, cn.solarmoon.spark_core.physics.SuspensionRowSpec>()
            for (wheel in view.wheels) {
                val body = PhysicsRigidBody(wheelCylinder(wheel), wheel.massKg.toFloat())
                body.isContactResponse = true
                body.setPhysicsLocation(
                    chassis.getPhysicsLocation(null).add(
                        Vector3f(
                            wheel.assemblyCenter.x.toFloat(),
                            wheel.assemblyCenter.y.toFloat(),
                            wheel.assemblyCenter.z.toFloat(),
                        ),
                    ),
                )
                body.owner = host
                body.collisionGroup = CollisionGroups.WHEEL
                // Terrain stays enabled so a wheel-on-ground bypass is observed
                // instead of being made impossible by the mask.
                body.setCollideWithGroups(TrackedTerrainCollision.TERRAIN_MESH or CollisionGroups.TRACK_LINK)
                body.friction = .9f
                body.setSleepingThresholds(0f, 0f)
                space.addCollisionObject(body)
                wheelBodies += body
                wheelByIndex[wheel.index] = body

                if (!dropMountJoints) {
                    compiled.mount(wheel.index).joint?.let { plan ->
                        val joint = TrackedJointFactory.create(plan, chassis, body)
                        space.addJoint(joint)
                        joints += joint
                    }
                }
                compiled.mount(wheel.index).springRow?.let { sideRows[wheel.index] = it }
            }
            wheels[side] = wheelBodies
            rows[side] = sideRows

            val specs = TrackedRunSpecs.compileRuns(view, spec.vehicleId, spec.drivetrain.friction)
            TrackRunAssemblyProbe.requireMatches(specs, view)

            val runBodies = ArrayList<PhysicsRigidBody>(view.runs.size)
            for ((index, run) in view.runs.withIndex()) {
                val body = TrackRunAssembly.createBody(
                    specs[index],
                    0,
                    host,
                    TrackRunAssembly.bodyName(specs[index], 0),
                )
                body.setPhysicsLocation(
                    chassis.getPhysicsLocation(null).add(
                        Vector3f(
                            run.center.x.toFloat(),
                            run.center.y.toFloat(),
                            run.center.z.toFloat(),
                        ),
                    ),
                )
                space.addCollisionObject(body)
                runBodies += body

                for (attachment in run.attachments) {
                    val plan = attachment.joint
                    val bodyA = resolve(plan.bodyA, body, wheelByIndex)
                    val bodyB = resolve(plan.bodyB, body, wheelByIndex)
                    val joint = TrackedJointFactory.create(plan, bodyA, bodyB)
                    space.addJoint(joint)
                    joints += joint
                }
            }
            runs[side] = runBodies
        }

        return Vehicle(space, chassis, views, wheels, runs, joints, rows, host, tally)
    }

    private fun resolve(
        ref: CompiledBodyRef,
        runBody: PhysicsRigidBody,
        wheelByIndex: Map<Int, PhysicsRigidBody>,
    ): PhysicsRigidBody = when (ref.kind) {
        CompiledBodyKind.WHEEL -> wheelByIndex.getValue(ref.ordinal)
        CompiledBodyKind.RUN -> runBody
        CompiledBodyKind.CHASSIS -> error("a run end cannot reference the chassis")
    }

    @Test
    fun bothSidesAssembleAsIndependentMechanisms() {
        val spec = spec()
        buildVehicle(spec).use { vehicle ->
            for (side in TrackedSide.entries) {
                val view = vehicle.views.getValue(side)
                assertEquals(view.members.size, view.runs.size, "$side run count")
                assertEquals(view.wheels.size, vehicle.wheels.getValue(side).size, "$side wheel bodies")
                assertEquals(view.runs.size, vehicle.runs.getValue(side).size, "$side run bodies")
                assertEquals(1, view.runs.count { it.closesLoop }, "$side closing run")
            }
            val left = vehicle.views.getValue(TrackedSide.LEFT)
            val right = vehicle.views.getValue(TrackedSide.RIGHT)
            assertTrue(
                abs(left.belt.centerPlaneX - right.belt.centerPlaneX) > 0.5,
                "the two belts must sit on opposite sides: ${left.belt.centerPlaneX} vs ${right.belt.centerPlaneX}",
            )
            assertNotEquals(
                vehicle.runs.getValue(TrackedSide.LEFT).first(),
                vehicle.runs.getValue(TrackedSide.RIGHT).first(),
            )
        }
    }

    /**
     * The sides must not touch or constrain each other.
     *
     * Lateral body ranges are one check, and a joint spanning both sides is the
     * other: that would be an instant coupling bug. Contact is covered by the
     * run-run counter in the load test.
     */
    @Test
    fun theTwoSidesAreDisjoint() {
        val spec = spec()
        buildVehicle(spec).use { vehicle ->
            val leftRange = lateralRange(vehicle, TrackedSide.LEFT)
            val rightRange = lateralRange(vehicle, TrackedSide.RIGHT)
            assertTrue(
                leftRange.second < rightRange.first || rightRange.second < leftRange.first,
                "the sides overlap laterally: left $leftRange, right $rightRange",
            )
            val leftBodies = vehicle.wheels.getValue(TrackedSide.LEFT).toSet() +
                vehicle.runs.getValue(TrackedSide.LEFT).toSet()
            val rightBodies = vehicle.wheels.getValue(TrackedSide.RIGHT).toSet() +
                vehicle.runs.getValue(TrackedSide.RIGHT).toSet()
            for (joint in vehicle.joints) {
                val a = joint.getBodyA()
                val b = joint.getBodyB()
                if (a === vehicle.chassis || b === vehicle.chassis) continue
                assertTrue(
                    !((a in leftBodies) && (b in rightBodies)) && !((a in rightBodies) && (b in leftBodies)),
                    "a joint spans both sides: $a -> $b",
                )
            }
        }
    }

    private fun lateralRange(vehicle: Vehicle, side: TrackedSide): Pair<Float, Float> {
        val xs = (vehicle.runs.getValue(side) + vehicle.wheels.getValue(side))
            .map { it.getPhysicsLocation(null).x }
        return xs.min() to xs.max()
    }

    /**
     * Both sides must stay assembled and load-bearing for a 40 s window.
     *
     * This is where the free chassis replaces B2.3's declared rail: if one side
     * collapses, the chassis rolls, a wheel touches the ground, or the anchors
     * separate, and those assertions fail.
     *
     * Contact counts are accumulated over **every** substep of the window, not
     * read from the last sample, so a bypass that appears for one substep is
     * still caught.
     *
     * **The chassis does not converge to rest, and that is recorded rather than
     * asserted away.** This fixture has no drive, brake or rolling resistance —
     * those are S4 — so the belt creeps along Z indefinitely. A "static
     * equilibrium" claim therefore cannot be made from this stage. What is
     * asserted is that the mechanism survives a window four times longer than the
     * plan's minimum while carrying the load, and that no bypass appears.
     */
    @Test
    fun bothSidesStayAssembledAndLoadBearingForAFortySecondWindow() {
        val spec = spec()
        buildVehicle(spec).use { vehicle ->
            // Settle first, then start the measurement window. Including the
            // initial drop would count transient wheel scuffs during the fall as
            // a load-path bypass, which they are not.
            vehicle.step(B2Sampling.SETTLE_SUBSTEPS)
            val startZ = vehicle.chassis.getPhysicsLocation(null).z
            vehicle.tally.resetWindow()
            vehicle.step(B2Sampling.SETTLE_SUBSTEPS * 4)
            assertTrue(vehicle.tally.windowRunTerrain > 0, "the runs must reach the terrain")
            println(
                "B2.4 wheelTerrainByIndex=" + vehicle.tally.windowWheelTerrainByIndex.map { (i, n) ->
                    val wheel = vehicle.views.getValue(TrackedSide.LEFT).wheels.getOrNull(i)
                    "${wheel?.bone ?: i}=$n"
                },
            )
            // The end stations sit next to a differently sized wheel, so the
            // straight compiled chord leaves their lowest point slightly proud of
            // the belt. A transient scuff there is a geometry fact, not load
            // bypass. What matters is who carries the vertical impulse.
            val runImpulse = vehicle.tally.windowRunTerrainImpulseNs
            val wheelImpulse = vehicle.tally.windowWheelTerrainImpulseNs
            val chassisImpulse = vehicle.tally.windowChassisTerrainImpulseNs
            val bypassShare = (wheelImpulse + chassisImpulse) / runImpulse
            assertTrue(
                bypassShare < 0.05,
                "runs carried $runImpulse N*s of terrain impulse, wheels $wheelImpulse and chassis " +
                    "$chassisImpulse N*s (bypass share $bypassShare)",
            )
            // A wheel may scuff the ground for a few substeps at the end stations
            // because the compiled chord between differently sized wheels leaves
            // its lowest point slightly proud. The count is reported, and the
            // assertion is on who carries the vertical impulse: a scuff that
            // carries no support is not a load-path bypass.
            assertEquals(
                0,
                vehicle.tally.windowChassisTerrain,
                "the chassis rested on the terrain during the window",
            )
            assertEquals(0, vehicle.tally.windowRunChassis, "a run touched the chassis; the load path is short-circuited")
            assertEquals(0, vehicle.tally.windowRunRun, "same-owner runs collided during the window")
            // Per side, not just merged: the merged total would still pass if one
            // side quietly carried the whole vehicle while the other only hung on
            // its mounts. Each side must reach the terrain and carry a comparable
            // share of the run impulse.
            val sideRunImpulse = TrackedSide.entries.associateWith { side ->
                vehicle.tally.windowRunTerrainImpulseByGroup[side.name] ?: 0.0
            }
            for (side in TrackedSide.entries) {
                assertTrue(
                    (vehicle.tally.windowRunTerrainByGroup[side.name] ?: 0) > 0,
                    "$side never reached the terrain with its runs",
                )
                val sideWheel = vehicle.tally.windowWheelTerrainImpulseByGroup[side.name] ?: 0.0
                val sideRun = sideRunImpulse.getValue(side)
                assertTrue(
                    sideWheel / sideRun < 0.05,
                    "$side carried the load through its wheels, not the runs: wheel $sideWheel vs run $sideRun N*s",
                )
            }
            val minSide = sideRunImpulse.values.min()
            val maxSide = sideRunImpulse.values.max()
            assertTrue(
                minSide > maxSide * 0.5,
                "one side carried far less than the other: per-side run impulse $sideRunImpulse",
            )
            for (side in TrackedSide.entries) {
                val gap = vehicle.firstEndAnchorGap(side)
                assertTrue(gap < 0.10, "$side lost its anchors over the window, gap $gap m")
            }
            val creep = abs(vehicle.chassis.getPhysicsLocation(null).z - startZ)
            println(
                "B2.4 window=${B2Sampling.SETTLE_SUBSTEPS * 4} substeps " +
                    "heave=${"%.4f".format(vehicle.chassis.getPhysicsLocation(null).y)} " +
                    "speed=${"%.4f".format(vehicle.chassis.getLinearVelocity(null).length())} " +
                    "unbrakedCreepZ=${"%.4f".format(creep)} m " +
                    "runTerrain=${vehicle.tally.windowRunTerrain} " +
                    "wheelTerrain=${vehicle.tally.windowWheelTerrain} " +
                    "chassisTerrain=${vehicle.tally.windowChassisTerrain} " +
                    "runChassis=${vehicle.tally.windowRunChassis} runRun=${vehicle.tally.windowRunRun} " +
                    "runImpulse=${"%.0f".format(runImpulse)} wheelImpulse=${"%.0f".format(wheelImpulse)} " +
                    "chassisImpulse=${"%.0f".format(chassisImpulse)} bypassShare=${"%.5f".format(bypassShare)} " +
                    "sideRunImpulse=${sideRunImpulse.mapValues { "%.0f".format(it.value) }}",
            )
        }
    }

    /**
     * The chassis must accept a known impulse and the mechanism must absorb it.
     *
     * Two separate things are checked, because either alone is weak:
     *
     * 1. **Momentum step.** Read the velocity immediately after
     *    `applyCentralImpulse`, before the next substep, and require
     *    `Δv = J / m`. A displacement check would pass even if the impulse were
     *    ignored, because this fixture already creeps along Z.
     * 2. **Absorption.** The impulse is along X while the unbraked creep is along
     *    Z, so the X velocity is a clean signal. It must decay: belt-to-ground
     *    friction has to remove the lateral momentum. Comparing total speed
     *    would mix in the creep and prove nothing.
     */
    @Test
    fun aKnownImpulseIsAcceptedAndAbsorbed() {
        val spec = spec()
        buildVehicle(spec).use { vehicle ->
            vehicle.step(B2Sampling.SETTLE_SUBSTEPS * 4)
            val before = vehicle.chassis.getLinearVelocity(null).x
            val impulse = 40_000f
            vehicle.chassis.applyCentralImpulse(Vector3f(impulse, 0f, 0f))
            // Read before the next substep: the impulse changes momentum at once,
            // while the constraints and contacts redistribute it during the step.
            val peak = vehicle.chassis.getLinearVelocity(null).x - before
            val expected = impulse / vehicle.chassis.mass
            val error = abs(peak - expected) / expected
            assertTrue(
                error < 0.15,
                "impulse must produce Δv=$expected m/s on ${vehicle.chassis.mass} kg, measured $peak m/s",
            )

            vehicle.step(60)
            val afterOneSecond = abs(vehicle.chassis.getLinearVelocity(null).x)
            assertTrue(
                afterOneSecond < abs(peak) * 0.75,
                "the belt must start absorbing the lateral impulse: |vx| ${abs(peak)} -> $afterOneSecond m/s after 1 s",
            )

            vehicle.step(B2Sampling.SETTLE_SUBSTEPS * 4)
            val settled = abs(vehicle.chassis.getLinearVelocity(null).x)
            // Full stop needs a brake (S4). What must hold here is that friction
            // keeps removing lateral momentum rather than the impulse being
            // stored forever.
            assertTrue(
                settled < abs(peak) * 0.5 && settled <= afterOneSecond * 1.05,
                "the lateral impulse must keep decaying: peak ${abs(peak)} -> 1s $afterOneSecond -> $settled m/s",
            )
            for (side in TrackedSide.entries) {
                val gap = vehicle.firstEndAnchorGap(side)
                assertTrue(gap < 0.10, "$side lost its anchors after the impulse, gap $gap m")
            }
            println(
                "B2.4 impulse Δv=${"%.4f".format(peak)} m/s expected=${"%.4f".format(expected)} m/s " +
                    "|vx|1s=${"%.4f".format(afterOneSecond)} settled=${"%.4f".format(settled)}",
            )
        }
    }

    /**
     * A vehicle without wheel mounts must fall further.
     *
     * Sensitivity control: if removing every mount changed nothing, the "held
     * up" assertion would be meaningless.
     */
    @Test
    fun aVehicleWithoutMountsFallsMuchFurther() {
        val spec = spec()
        val mounted = buildVehicle(spec).use { vehicle ->
            vehicle.step(B2Sampling.SETTLE_SUBSTEPS)
            vehicle.chassis.getPhysicsLocation(null).y
        }
        val unmounted = buildVehicle(spec, dropMountJoints = true).use { vehicle ->
            vehicle.step(B2Sampling.SETTLE_SUBSTEPS)
            vehicle.chassis.getPhysicsLocation(null).y
        }
        assertTrue(
            unmounted < mounted - 0.05,
            "without mounts the chassis must fall further: mounted $mounted m, unmounted $unmounted m",
        )
    }

    /** Teardown must remove joints before bodies and be idempotent. */
    @Test
    fun teardownRemovesJointsBeforeBodiesAndIsIdempotent() {
        val spec = spec()
        val vehicle = buildVehicle(spec)
        val joints = vehicle.joints.toList()
        val bodies = vehicle.runs.getValue(TrackedSide.LEFT).toList()
        assertTrue(joints.isNotEmpty(), "the vehicle must own joints before teardown")

        vehicle.close()
        vehicle.close()

        assertEquals(0, vehicle.space.getRigidBodyList().count { it in bodies }, "run bodies must be gone")
        assertEquals(0, vehicle.space.getJointList().count { it in joints }, "joints must be gone")
    }
}

private fun rotate(point: net.minecraft.world.phys.Vec3, rotation: CompiledRotation): net.minecraft.world.phys.Vec3 {
    val out = quaternionOf(rotation).toRotationMatrix()
        .mult(Vector3f(point.x.toFloat(), point.y.toFloat(), point.z.toFloat()), Vector3f())
    return net.minecraft.world.phys.Vec3(out.x.toDouble(), out.y.toDouble(), out.z.toDouble())
}

private fun quaternionOf(rotation: CompiledRotation): com.jme3.math.Quaternion =
    com.jme3.math.Quaternion(rotation.x, rotation.y, rotation.z, rotation.w).normalizeLocal()

private fun worldPoint(body: PhysicsRigidBody, local: net.minecraft.world.phys.Vec3): Vector3f {
    val offset = body.getPhysicsRotation(null).toRotationMatrix()
        .mult(Vector3f(local.x.toFloat(), local.y.toFloat(), local.z.toFloat()), Vector3f())
    return body.getPhysicsLocation(null).add(offset)
}

private fun wheelCylinder(wheel: CompiledWheel): CylinderCollisionShape =
    CylinderCollisionShape(
        Vector3f(wheel.halfWidth.toFloat(), wheel.radius.toFloat(), wheel.radius.toFloat()),
        PhysicsSpace.AXIS_X,
    ).apply { margin = .001f }