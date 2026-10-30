package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.NativePhysicsLoader
import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.PhysicsSuspensionRow
import cn.solarmoon.spark_core.physics.SuspensionRowState
import cn.solarmoon.spark_core.physics.body.CollisionGroups
import cn.solarmoon.spark_core.physics.body.PhysicsJoints
import cn.solarmoon.spark_core.physics.body.owner
import com.jme3.bullet.RotationOrder
import com.jme3.math.Matrix3f
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

/**
 * B2.3: the vertical load path through one side.
 *
 * ```text
 *   chassis weight (dynamic chassis on a declared vertical rail)
 *     -> suspension row (compiled bilateral SI row)
 *     -> wheel body
 *     -> first-end revolute / second-end single-normal constraint
 *     -> run bodies created by TrackRunAssembly
 *     -> terrain (static AABB-equivalent floor)
 * ```
 *
 * ## Why the chassis slides on a vertical rail
 *
 * A single track side cannot hold a chassis upright: nothing balances the roll
 * moment about the belt. Letting a free chassis fall onto one side therefore
 * measures a tip-over, not a load path. The chassis is still **dynamic and
 * carries the sprung mass**, but it is constrained by a vertical slider to a
 * static rail, so it can only settle downward under its own weight. The rail
 * transmits no vertical load: it only removes the roll/pitch degrees of freedom
 * that a one-sided fixture cannot balance. This is a declared fixture boundary,
 * and the load carried is still the chassis's own weight.
 *
 * Consequently this stage does **not** claim "the chassis is held up"; it claims
 * the weight reaches the ground through the runs. That is B2.4, where both sides
 * exist and the chassis is free.
 *
 * ## Mass basis
 *
 * The compiled row of one side is sized for one side's share, so the applied
 * load is `sprungAssemblyMassKg / 2`. Using the whole sprung assembly on one
 * side would load every row at twice its design share.
 *
 * ## Origin
 *
 * Compiled wheel and run positions are chassis-local. The chassis body sits at a
 * chosen world pose, and every body is placed at `chassisWorld + compiledLocal`.
 * The belt plane is never used as a substitute for the chassis origin.
 *
 * No threshold is frozen: the assertions are structural and the measured values
 * are printed for later confirmation.
 */
class TrackedChassisLoadPathNativeTest {

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

    private class Rig(
        val space: PhysicsSpace,
        val view: CompiledTrackSide,
        val chassis: PhysicsRigidBody,
        /** Sprung mass this single-side fixture carries, kg. */
        val sprungMassKg: Float,
        val wheels: List<PhysicsRigidBody>,
        val runs: List<PhysicsRigidBody>,
        val endJoints: List<New6Dof>,
        val mountJoints: List<New6Dof>,
        val rows: Map<Int, cn.solarmoon.spark_core.physics.SuspensionRowSpec>,
        val tally: TrackedMechanismFixture.ContactTally,
    ) : AutoCloseable {
        /** Vertical slider to the static rail; not part of the vehicle. */
        lateinit var railJoint: New6Dof

        val appliedLoadN: Float get() = sprungMassKg * 9.81f

        override fun close() {
            mountJoints.forEach { space.removeJoint(it) }
            endJoints.forEach { space.removeJoint(it) }
            if (::railJoint.isInitialized) space.removeJoint(railJoint)
            space.destroy()
        }

        fun wheelBody(wheelIndex: Int) = wheels[view.wheels.indexOfFirst { it.index == wheelIndex }]

        /**
         * Travel of one station, metres, positive in compression.
         *
         * Measured in the mount joint's frame: the wheel origin expressed in the
         * chassis joint basis, relative to the frame anchor.
         */
        fun travelOf(wheel: CompiledWheel): Float {
            val body = wheelBody(wheel.index)
            val plan = view.mount(wheel.index).joint ?: return 0f
            val chassisInverse = chassis.getPhysicsRotation(null).inverse().toRotationMatrix()
            val relative = body.getPhysicsLocation(null).subtract(chassis.getPhysicsLocation(null))
            val inChassisFrame = chassisInverse.mult(relative, Vector3f())
            val anchor = plan.frameInA.origin
            val fromAnchor = inChassisFrame.subtract(
                Vector3f(anchor.x.toFloat(), anchor.y.toFloat(), anchor.z.toFloat()),
            )
            val frameInverse = jmeFromCompiled(plan.frameInA.rotation).inverse().toRotationMatrix()
            return frameInverse.mult(fromAnchor, Vector3f()).y
        }

        fun rowAxis(wheel: CompiledWheel): Vector3f {
            val plan = view.mount(wheel.index).joint
            val local = if (plan == null) {
                Vector3f(0f, 1f, 0f)
            } else {
                jmeFromCompiled(plan.frameInA.rotation).toRotationMatrix().mult(Vector3f(0f, 1f, 0f), Vector3f())
            }
            return chassis.getPhysicsRotation(null).toRotationMatrix().mult(local, Vector3f()).normalize()
        }

        /** Signed row force currently carried by one station, newtons. */
        fun rowForce(wheel: CompiledWheel): Float {
            val row = rows[wheel.index] ?: return 0f
            val body = wheelBody(wheel.index)
            // The damper acts on the *relative* velocity. Using the wheel's world
            // velocity alone would add the chassis motion (the rail lets it move
            // vertically) to the damping term and corrupt the measured row force.
            val velocity = body.getLinearVelocity(null)
                .subtract(chassis.getLinearVelocity(null))
                .dot(rowAxis(wheel))
            return PhysicsSuspensionRow.force(row, SuspensionRowState(travelOf(wheel), velocity))
        }

        fun totalRowForceN(): Double = view.wheels.sumOf { rowForce(it).toDouble() }

        /**
         * One substep: apply the declared chassis load, then every station's row.
         *
         * The chassis is kinematic, so its share of the paired force does nothing;
         * the wheel's share is what the belt has to carry. Keeping the paired
         * call documents that the row is an internal action-reaction pair rather
         * than a one-way push.
         */
        fun step(substeps: Int) {
            repeat(substeps) {
                for (wheel in view.wheels) {
                    if (!rows.containsKey(wheel.index)) continue
                    val body = wheelBody(wheel.index)
                    val force = PhysicsSuspensionRow.force(
                        rows.getValue(wheel.index),
                        SuspensionRowState(
                            travelOf(wheel),
                            body.getLinearVelocity(null)
                                .subtract(chassis.getLinearVelocity(null))
                                .dot(rowAxis(wheel)),
                        ),
                    )
                    PhysicsJoints.applyPairedForce(
                        chassis,
                        body,
                        rowAxis(wheel),
                        force,
                        body.getPhysicsLocation(null),
                    )
                }
                space.update(1f / 60f, 0, false, true, false)
                tally.sample(space, wheels, runs, chassis)
            }
        }
    }

    private fun buildRig(spec: TrackedVehicleSpec): Rig {
        val compiled = spec.side(TrackedSide.LEFT)
        val members = compiled.members
        val belt = compiled.belt
        val byIndex = compiled.wheels.associateBy { it.index }
        val masses = TrackedMassAllocator.allocateRuns(members, byIndex, sideBudgetKg = compiled.runMassTotalKg)
        val runs = TrackedRunGeometry.compile(members, byIndex, belt, masses)
        val view = CompiledTrackSide(
            side = TrackedSide.LEFT,
            wheels = compiled.wheels,
            belt = belt,
            beltOrder = compiled.beltOrder,
            members = members,
            runs = runs,
            mounts = compiled.mounts,
            suspension = compiled.suspension,
            legacy = null,
        )

        val space = PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT)
        space.setGravity(Vector3f(0f, -9.81f, 0f))
        val host = object : PhysicsHost {
            override val physicsLevel: cn.solarmoon.spark_core.physics.level.PhysicsLevel
                get() = throw UnsupportedOperationException("native fixture has no PhysicsLevel")
            override val allPhysicsBodies: MutableMap<String, PhysicsCollisionObject> =
                java.util.concurrent.ConcurrentHashMap()
        }
        val tally = TrackedMechanismFixture.ContactTally()

        // A flat, static floor: the AABB-equivalent stand-in for terrain. Real
        // Minecraft VoxelShape collision is S3 and is not claimed here.
        val floor = PhysicsRigidBody(BoxCollisionShape(60f, 0.5f, 60f), 0f)
        floor.setPhysicsLocation(Vector3f(0f, -8f, 0f))
        floor.friction = .9f
        floor.collisionGroup = TrackedTerrainCollision.TERRAIN_MESH
        floor.setCollideWithGroups(CollisionGroups.NONE)
        space.addCollisionObject(floor)

        val sideSprungMass = (spec.sprungAssemblyMassKg / 2.0).toFloat()
        // Diagnostic hull proxy. Its half height is chosen so the box lies between
        // the top run (local y about +0.42) and the lower runs (about -0.63 and
        // below): the belt must not intersect the hull at bind. This is not the
        // audited vehicle OBB; it is a declared proxy that makes a run-chassis
        // contact meaningful instead of guaranteed.
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.8f, 0.25f, 1.2f), sideSprungMass)
        chassis.setPhysicsLocation(Vector3f(0f, -6.30f, 0f))
        chassis.owner = host
        chassis.collisionGroup = CollisionGroups.CHASSIS
        // Terrain collision stays ON so a chassis-on-ground bypass is *observed*
        // rather than made impossible by the mask. Track links stay off: the
        // chassis must ride the springs, not rest on the belt.
        chassis.setCollideWithGroups(TrackedTerrainCollision.TERRAIN_MESH)
        space.addCollisionObject(chassis)

        val wheelBodies = ArrayList<PhysicsRigidBody>(view.wheels.size)
        val mountJoints = ArrayList<New6Dof>(view.wheels.size)
        val rows = HashMap<Int, cn.solarmoon.spark_core.physics.SuspensionRowSpec>()
        for (wheel in view.wheels) {
            val mount = compiled.mount(wheel.index)
            val wheelBody = PhysicsRigidBody(wheelCylinder(wheel), wheel.massKg.toFloat())
            wheelBody.isContactResponse = true
            wheelBody.setPhysicsLocation(
                chassis.getPhysicsLocation(null).add(
                    Vector3f(
                        wheel.assemblyCenter.x.toFloat(),
                        wheel.assemblyCenter.y.toFloat(),
                        wheel.assemblyCenter.z.toFloat(),
                    ),
                ),
            )
            wheelBody.friction = .9f
            wheelBody.owner = host
            wheelBody.collisionGroup = CollisionGroups.WHEEL
            // Terrain collision stays ON so a wheel-on-ground bypass is observed.
            // The belt geometry must keep the wheel clear; the test checks that
            // rather than disabling the contact and declaring success.
            wheelBody.setCollideWithGroups(TrackedTerrainCollision.TERRAIN_MESH or CollisionGroups.TRACK_LINK)
            wheelBody.setSleepingThresholds(0f, 0f)
            space.addCollisionObject(wheelBody)
            wheelBodies += wheelBody

            mount.joint?.let { plan ->
                val joint = TrackedJointFactory.create(plan, chassis, wheelBody)
                space.addJoint(joint)
                mountJoints += joint
            }
            mount.springRow?.let { rows[wheel.index] = it }
        }
        val byWheelIndex = view.wheels.indices.associate { view.wheels[it].index to wheelBodies[it] }

        val specs = TrackedRunSpecs.compileRuns(view, spec.vehicleId, spec.drivetrain.friction)
        TrackRunAssemblyProbe.requireMatches(specs, view)

        val runBodies = ArrayList<PhysicsRigidBody>(runs.size)
        val endJoints = ArrayList<New6Dof>(runs.size * 2)
        for ((index, run) in runs.withIndex()) {
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
                val bodyA = resolve(plan.bodyA, body, byWheelIndex)
                val bodyB = resolve(plan.bodyB, body, byWheelIndex)
                val joint = TrackedJointFactory.create(plan, bodyA, bodyB)
                space.addJoint(joint)
                endJoints += joint
            }
        }

        // Vertical slider: a static rail the chassis slides on. All translations
        // but Y and all rotations are locked, so the one-sided fixture cannot tip
        // over while still carrying its own weight on the springs.
        val rail = PhysicsRigidBody(BoxCollisionShape(0.05f), 0f)
        rail.isKinematic = true
        rail.setPhysicsLocation(chassis.getPhysicsLocation(null))
        // jme3 defaults a body's collision group to bit 0, which is Spark's
        // TERRAIN. Without this the rail box sits inside the chassis and the
        // chassis (which must collide with terrain) explodes on the first step.
        rail.collisionGroup = CollisionGroups.PHYSICS_BODY
        rail.setCollideWithGroups(CollisionGroups.NONE)
        space.addCollisionObject(rail)
        val railJoint = New6Dof(
            rail,
            chassis,
            Vector3f(0f, 0f, 0f),
            Vector3f(0f, 0f, 0f),
            Matrix3f(),
            Matrix3f(),
            RotationOrder.XYZ,
        )
        PhysicsJoints.lock(railJoint, PhysicsJoints.LIN_X)
        // Y must be genuinely free: a finite window would let the rail itself
        // carry the chassis once it hits the bound, which is exactly the bypass
        // this fixture must not have.
        PhysicsJoints.free(railJoint, PhysicsJoints.LIN_Y)
        PhysicsJoints.lock(railJoint, PhysicsJoints.LIN_Z)
        for (axis in 0..2) {
            PhysicsJoints.lock(
                railJoint,
                PhysicsJoints.ANG_X + axis,
            )
        }
        space.addJoint(railJoint)

        val rig = Rig(space, view, chassis, sideSprungMass, wheelBodies, runBodies, endJoints, mountJoints, rows, tally)
        rig.railJoint = railJoint
        return rig
    }

    private fun resolve(
        ref: CompiledBodyRef,
        runBody: PhysicsRigidBody,
        byWheelIndex: Map<Int, PhysicsRigidBody>,
    ): PhysicsRigidBody = when (ref.kind) {
        CompiledBodyKind.WHEEL -> byWheelIndex.getValue(ref.ordinal)
        CompiledBodyKind.RUN -> runBody
        CompiledBodyKind.CHASSIS -> error("a run end cannot reference the chassis")
    }

    /**
     * The sprung rows must carry most of the chassis weight, inside their window.
     *
     * Exact equality is **not** expected, and asserting it would be wrong: the
     * idler and the drive sprocket have fixed mounts, so part of the chassis
     * weight reaches the belt through those mounts rather than through a spring.
     * What must hold is that the sprung stations carry the dominant share and
     * stay inside their declared travel.
     */
    @Test
    fun theRowsCarryMostOfTheChassisWeightInsideTheirWindow() {
        val spec = spec()
        buildRig(spec).use { rig ->
            rig.step(600)
            val travels = rig.view.wheels.mapNotNull { wheel ->
                rig.rows[wheel.index]?.let { row -> rig.travelOf(wheel).toDouble() to row }
            }
            assertTrue(travels.isNotEmpty(), "no sprung stations were instrumented")
            for ((travel, row) in travels) {
                assertTrue(travel <= row.travelUpM + 0.02, "travel $travel m exceeds ${row.travelUpM} m")
                assertTrue(travel >= -row.travelDownM - 0.02, "travel $travel m exceeds -${row.travelDownM} m")
            }
            val support = rig.totalRowForceN()
            val weight = rig.sprungMassKg * 9.81
            val share = support / weight
            assertTrue(
                share in 0.5..1.15,
                "sprung rows carry $support N of the $weight N chassis weight (share $share); " +
                    "either the rows are not carrying the load or something is propping the chassis",
            )
        }
    }

    /**
     * The load must reach the floor through the runs.
     *
     * Wheels and chassis are allowed to touch the terrain; the counters prove
     * they did not, rather than a collision mask making the check unfalsifiable.
     * Counts are accumulated over the whole window, so a bypass that appears for
     * one substep still fails the test.
     */
    @Test
    fun theLoadReachesTheTerrainThroughTheRuns() {
        val spec = spec()
        buildRig(spec).use { rig ->
            rig.step(600)
            assertTrue(rig.tally.windowRunTerrain > 0, "the runs must reach the terrain")
            assertEquals(0, rig.tally.windowWheelTerrain, "a wheel reached the terrain; the load bypassed the runs")
            assertEquals(0, rig.tally.windowChassisTerrain, "the chassis reached the terrain; the load bypassed the runs")
            assertEquals(0, rig.tally.windowRunRun, "same-owner runs must not collide")
            assertEquals(0, rig.tally.windowRunChassis, "a run touched the chassis; the load path is short-circuited")
        }
    }

    /**
     * Terrain support must equal the assembly weight.
     *
     * System-boundary check: the only external loads are the applied chassis
     * load and gravity on the running gear; the only upward load is the run's
     * terrain contact. Internal row and constraint forces cancel inside the
     * boundary and are not counted.
     */
    @Test
    fun theRunTerrainSupportCarriesTheWholeAssembly() {
        val spec = spec()
        buildRig(spec).use { rig ->
            rig.step(600)
            rig.tally.resetWindow()
            val window = 180
            var impulseSum = 0.0
            repeat(window) {
                rig.step(1)
                impulseSum += rig.tally.runTerrainImpulseNs
            }
            val support = impulseSum / (window * (1.0 / 60.0))
            val gearMass = rig.view.wheels.sumOf { it.massKg } + rig.view.runs.sumOf { it.massKg }
            val expected = (rig.sprungMassKg + gearMass).toFloat() * 9.81f
            val closure = abs(support - expected) / expected
            // A wheel or chassis touching the floor during the sampling window
            // would carry part of the load outside the measured run contact.
            assertEquals(0, rig.tally.windowWheelTerrain, "a wheel carried load during the sampling window")
            assertEquals(0, rig.tally.windowChassisTerrain, "the chassis carried load during the sampling window")
            assertTrue(
                closure < 0.2,
                "terrain support $support N vs chassis + gear weight $expected N, relative closure $closure",
            )
        }
    }

    /** Report the measured numbers; no verdict. */
    @Test
    fun theLoadPathMeasurementsAreReported() {
        val spec = spec()
        buildRig(spec).use { rig ->
            rig.step(600)
            val travels = rig.view.wheels.mapNotNull { wheel ->
                rig.rows[wheel.index]?.let { rig.travelOf(wheel).toDouble() }
            }
            println(
                "B2.3 load path chassisY=${"%.4f".format(rig.chassis.getPhysicsLocation(null).y)} " +
                    "runs=${rig.runs.size} wheels=${rig.wheels.size} " +
                    "runTerrain=${rig.tally.runTerrain} wheelTerrain=${rig.tally.wheelTerrain} " +
                    "chassisTerrain=${rig.tally.chassisTerrain} runRun=${rig.tally.runRun} " +
                    "runChassis=${rig.tally.runChassis} windowSamples=${rig.tally.windowSamples} " +
                    "rowSupport=${"%.0f".format(rig.totalRowForceN())} " +
                    "chassisWeight=${"%.0f".format(rig.sprungMassKg * 9.81)} " +
                    "travels=${travels.map { "%.4f".format(it) }}",
            )
            assertEquals(rig.rows.size, travels.size, "every sprung station must be measured")
        }
    }
}

private fun wheelCylinder(wheel: CompiledWheel): CylinderCollisionShape =
    CylinderCollisionShape(
        Vector3f(wheel.halfWidth.toFloat(), wheel.radius.toFloat(), wheel.radius.toFloat()),
        PhysicsSpace.AXIS_X,
    ).apply { margin = .001f }

private fun jmeFromCompiled(rotation: CompiledRotation): com.jme3.math.Quaternion =
    com.jme3.math.Quaternion(rotation.x, rotation.y, rotation.z, rotation.w).normalizeLocal()