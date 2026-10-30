package com.atsuishio.superbwarfare.gametest

import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.SparkPhysics
import cn.solarmoon.spark_core.physics.body.CollisionGroups
import cn.solarmoon.spark_core.physics.body.collideWithOwnerGroups
import cn.solarmoon.spark_core.physics.body.onPostPhysicsTick
import cn.solarmoon.spark_core.physics.body.onPrePhysicsTick
import cn.solarmoon.spark_core.physics.body.owner
import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.physics.VehiclePhysics
import com.atsuishio.superbwarfare.physics.tracked.TrackedSide
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleCompiler
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleDefinition
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleRuntime
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleSpec
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import kotlinx.serialization.json.Json
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * S3.1 and S4 gates on real Minecraft terrain.
 *
 * S3.1: the belt must rest on actual block `VoxelShape` collision, not on a
 * fixture floor. The floor here is placed with `setBlock`, so the only thing
 * holding the vehicle up is the real AABB terrain path.
 *
 * S4: contact motion drives, brakes, coasts and steers. These cases claim the
 * displacement the belt actually produces. They do not claim a slip band,
 * a free tip-over, or a long soak.
 */
@GameTestHolder(Mod.MODID)
@PrefixGameTestTemplate(false)
object TrackedTerrainAndDriveGameTests {

    /** Support-wheel block span relative to a hull centred on the structure origin. */
    private const val CONTACT_Z_MIN = -3
    private const val CONTACT_Z_MAX = 3

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun resource(path: String): String {
        val stream = TrackedTerrainAndDriveGameTests::class.java.classLoader.getResourceAsStream(path)
            ?: error("missing classpath resource $path")
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun compile(): TrackedVehicleSpec = TrackedVehicleCompiler.compile(
        "superbwarfare:ztz_99a",
        json.decodeFromString(
            TrackedVehicleDefinition.serializer(),
            resource("data/superbwarfare/sbw/tracked/ztz_99a.json"),
        ),
        resource("assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json"),
    ).requireSpec()

    private fun host(physics: PhysicsLevel) = object : PhysicsHost {
        override val physicsLevel: PhysicsLevel get() = physics
        override val allPhysicsBodies: MutableMap<String, PhysicsCollisionObject> = ConcurrentHashMap()
    }

    /**
     * Run one physics-thread substep of the runtime.
     *
     * A chain of `submitTask { step() }` is **not** N Minecraft ticks: the
     * physics worker drains the whole queue in one processTasks pass, so those
     * steps would all run between solvers. One explicit step is enough to
     * publish the control demand this test asserts.
     */
    private fun stepOnce(
        physics: PhysicsLevel,
        runtime: TrackedVehicleRuntime,
    ): java.util.concurrent.CompletableFuture<Void> =
        physics.submitTask("s4 step") { runtime.step() }.thenApply<Void> { null }

    /**
     * Lay a floor of [block] so the belt rests on that collision shape.
     *
     * Keep the footprint inside this test's cell. `empty.nbt` is 32×8×32, which
     * puts the next test about 36 blocks away. A radius past that writes stone
     * into the neighbour.
     */
    private fun floor(helper: GameTestHelper, radiusX: Int, radiusZ: Int, block: Block = Blocks.STONE) {
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -radiusX..radiusX) {
            for (z in -radiusZ..radiusZ) {
                helper.getLevel().setBlock(anchor.offset(x, 0, z), block.defaultBlockState(), 3)
                for (y in 1..8) {
                    helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
                }
            }
        }
    }

    private data class MovingHull(
        val physics: PhysicsLevel,
        val chassis: PhysicsRigidBody,
        val runtime: java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>,
        val startZ: java.util.concurrent.atomic.AtomicReference<Double>,
        val setup: java.util.concurrent.CompletableFuture<*>,
    )

    /**
     * Shared flat-floor fixture for the S4 control tests. The chassis is
     * deliberately excluded from terrain so the belt must produce the motion;
     * the runtime itself still uses the local Bullet terrain mesh.
     */
    private fun movingHull(helper: GameTestHelper, label: String): MovingHull {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 5, 10)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 8.0f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(
            BoxCollisionShape(0.9f, 0.4f, 1.6f),
            spec.sprungAssemblyMassKg.toFloat(),
        )
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val created = TrackedVehicleRuntime(physics, host, spec, chassis)
        created.contactDriveEnabled = true
        runtime.set(created)
        val startZ = java.util.concurrent.atomic.AtomicReference(origin.z.toDouble())
        val setup = physics.submitTask("$label setup") {
            chassis.owner = host
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            physics.addBody(chassis)
            startZ.set(origin.z.toDouble())
        }
        return MovingHull(physics, chassis, runtime, startZ, setup)
    }

    /**
     * S3.1: the belt rests on real block AABB collision.
     *
     * The chassis is dynamic and free. If the tracked gear could not carry it on
     * real terrain, the hull would fall through the floor; if the terrain path
     * were a fixture stand-in, this would not exercise it at all.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun theBeltRestsOnRealBlockTerrain(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 6, 8)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(
            BoxCollisionShape(0.9f, 0.4f, 1.6f),
            (spec.sprungAssemblyMassKg / 2.0).toFloat(),
        )
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val startY = java.util.concurrent.atomic.AtomicReference(0.0)

        val setup = physics.submitTask("s3 terrain setup") {
            chassis.owner = host
            chassis.collisionGroup = CollisionGroups.CHASSIS
            // The hull must not touch terrain. Resting height only proves the belt
            // when the chassis mask cannot carry the weight itself.
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            physics.addBody(chassis)
            startY.set(origin.y.toDouble())
        }

        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "the terrain setup task did not run")
                setup.join().let { }
                helper.assertTrue(chassis.isInWorld, "the chassis must be in the world")
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil {
                helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble")
            }
            // Let it settle onto the real blocks.
            .thenIdle(120)
            .thenExecute {
                val y = chassis.getPhysicsLocation(null).y.toDouble()
                helper.assertTrue(
                    y < startY.get() - 0.05,
                    "the vehicle must settle onto the terrain, started ${startY.get()} ended $y",
                )
                helper.assertTrue(
                    y > startY.get() - 2.5,
                    "the vehicle fell through the terrain: started ${startY.get()} ended $y",
                )
            }
            // `beltSupported` is cleared at the start of every substep, so a
            // single read from the server thread can land in that window even
            // while the belt is resting on the floor. Wait for a substep that
            // actually recorded the contact instead of sampling once.
            .thenWaitUntil {
                helper.assertTrue(
                    runtime.get()?.beltSupported() == true,
                    "the belt, not the chassis, must be the terrain contact",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil {
                helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close")
            }
            .thenExecute { physics.submitTask("s3 terrain cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * S4: the control law publishes a forward drive demand, and braking asks
     * for zero surface speed.
     *
     * This is **not** a traction proof. Contact drive is gated off until the
     * pre-solver timing is proven; the test only checks that the runtime
     * consumes the same controller the production path will use.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun driveMovesTheBeltForwardAndBrakingOpposesIt(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 5, 8)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), 0f)
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val startZ = java.util.concurrent.atomic.AtomicReference(0.0)
        val afterDrive = java.util.concurrent.atomic.AtomicReference(0.0)
        val leftDemand = java.util.concurrent.atomic.AtomicReference(0.0)
        val rightDemand = java.util.concurrent.atomic.AtomicReference(0.0)

        val setup = physics.submitTask("s4 drive setup") {
            chassis.isKinematic = true
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.PHYSICS_BODY
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.owner = host
            physics.addBody(chassis)
            startZ.set(origin.z.toDouble())
        }

        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "the drive setup task did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                runtime.set(created)
                created.contactDriveEnabled = false
                created.create()
            }
            .thenWaitUntil {
                helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble")
            }
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                created.setDriveCommand(
                    TrackedVehicleRuntime.DriveCommand(
                        throttle = 1f,
                        steer = 0f,
                        brake = false,
                        powered = true,
                    ),
                )
                stepOnce(physics, created)
            }
            .thenIdle(5)
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                val demand = created.lastDemand()
                helper.assertTrue(
                    demand.state == com.atsuishio.superbwarfare.physics.tracked.TrackedDriveController.DriveState.DRIVE,
                    "straight throttle must be DRIVE, was ${demand.state}",
                )
                val (left, right) = created.lastDriveDemand()
                leftDemand.set(left)
                rightDemand.set(right)
                helper.assertTrue(
                    left > 0.0 && right > 0.0,
                    "straight throttle must drive both sides forward, got left=$left right=$right",
                )
                helper.assertTrue(
                    abs(left - right) < 0.05,
                    "straight throttle must not steer, got left=$left right=$right",
                )
                helper.assertTrue(
                    created.driveForceN() == 0f,
                    "contact drive is gated off; no net force may be applied, measured ${created.driveForceN()} N",
                )
                afterDrive.set(demand.targetSpeedMps)
            }
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                created.setDriveCommand(
                    TrackedVehicleRuntime.DriveCommand(throttle = 1f, brake = true, powered = true),
                )
                stepOnce(physics, created)
            }
            .thenIdle(5)
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                val braked = created.lastDemand()
                helper.assertTrue(
                    braked.state == com.atsuishio.superbwarfare.physics.tracked.TrackedDriveController.DriveState.BRAKE,
                    "braking must be BRAKE, was ${braked.state}",
                )
                helper.assertTrue(
                    braked.targetSpeedMps == 0.0,
                    "braking must ask for zero surface speed, was ${braked.targetSpeedMps}",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil {
                helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close")
            }
            .thenExecute { physics.submitTask("s4 drive cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * S4: differential steering must split the sides.
     *
     * A right turn drives the left belt faster and the right belt slower. If the
     * split were missing, skid steering would be impossible regardless of how
     * much torque the drivetrain has.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun steeringSplitsTheTwoSides(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 5, 8)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), 0f)
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)

        val setup = physics.submitTask("s4 steer setup") {
            chassis.isKinematic = true
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.PHYSICS_BODY
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.owner = host
            physics.addBody(chassis)
        }

        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "the steer setup task did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil {
                helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble")
            }
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                created.setDriveCommand(
                    TrackedVehicleRuntime.DriveCommand(throttle = 0.4f, steer = 1f, powered = true),
                )
                stepOnce(physics, created)
            }
            .thenIdle(10)
            .thenExecute {
                val (left, right) = runtime.get()?.lastDriveDemand() ?: (0.0 to 0.0)
                helper.assertTrue(
                    left > right,
                    "a right turn must drive the left side harder, got left=$left right=$right",
                )
                val compiled = spec.side(TrackedSide.LEFT)
                helper.assertTrue(compiled.wheels.any { it.receivesDrive }, "the left side must have a drive wheel")
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 steer cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * S4 contact path: a forward demand written as pre-solver contact motion
     * must move the hull. The hull does not collide with the floor, so the only
     * traction available is the belt.
     */
    @JvmStatic
    @GameTest(template = "empty", batch = "solo-forward-drive", timeoutTicks = 900)
    fun contactMotionDrivesTheHullForward(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 5, 10)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 8.0f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(
            BoxCollisionShape(0.9f, 0.4f, 1.6f),
            spec.sprungAssemblyMassKg.toFloat(),
        )
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val startZ = java.util.concurrent.atomic.AtomicReference(0.0)

        val setup = physics.submitTask("s4 contact setup") {
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.owner = host
            physics.addBody(chassis)
            startZ.set(origin.z.toDouble())
        }

        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "the contact setup task did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                runtime.set(created)
                created.contactDriveEnabled = true
                created.create()
            }
            .thenWaitUntil {
                helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble")
            }
            .thenIdle(40)
            .thenExecute {
                runtime.get()?.setDriveCommand(
                    TrackedVehicleRuntime.DriveCommand(throttle = 1f, powered = true),
                )
            }
            .thenIdle(100)
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                val z = chassis.getPhysicsLocation(null).z.toDouble()
                helper.assertTrue(
                    created.contactMotionCount() > 0,
                    "belt-terrain contacts must receive a motion target, count ${created.contactMotionCount()} " +
                        created.terrainDiagnostics(),
                )
                helper.assertTrue(
                    z > startZ.get() + 0.25,
                    "forward contact motion must move the hull toward +Z, start ${startZ.get()} now $z " +
                        created.terrainDiagnostics(),
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 contact cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /** Reverse throttle must move the hull toward -Z, the opposite of the visual nose. */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun contactMotionReversesTheHull(helper: GameTestHelper) {
        val (physics, chassis, runtime, startZ, setup) = movingHull(helper, "s4 reverse")
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "reverse setup did not run")
                setup.join().let { }
            }
            .thenExecute { runtime.get()?.create() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(30)
            .thenExecute {
                runtime.get()?.setDriveCommand(
                    TrackedVehicleRuntime.DriveCommand(throttle = -1f, powered = true),
                )
            }
            .thenIdle(160)
            .thenExecute {
                val z = chassis.getPhysicsLocation(null).z.toDouble()
                helper.assertTrue(
                    (runtime.get()?.lastContactTarget() ?: 0f) < 0f,
                    "reverse must request a backward contact target, got ${runtime.get()?.lastContactTarget()}",
                )
                helper.assertTrue(
                    z < startZ.get() - 0.25,
                    "reverse contact motion must move the hull toward -Z, start ${startZ.get()} now $z " +
                        runtime.get()?.terrainDiagnostics(),
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 reverse cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /** Braking must cut the forward speed the contact drive just produced. */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun brakingCutsTheForwardSpeed(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 6, 10)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val startZ = java.util.concurrent.atomic.AtomicReference(origin.z.toDouble())
        val setup = physics.submitTask("s4 brake setup") {
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.owner = host
            physics.addBody(chassis)
        }
        val createdRuntime = TrackedVehicleRuntime(physics, host, spec, chassis)
        createdRuntime.contactDriveEnabled = true
        runtime.set(createdRuntime)
        val drivenZ = java.util.concurrent.atomic.AtomicReference(0.0)
        val drivenSpeed = java.util.concurrent.atomic.AtomicReference(0.0)
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "brake setup did not run")
                setup.join().let { }
            }
            .thenExecute { runtime.get()?.create() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(30)
            .thenExecute {
                runtime.get()?.setDriveCommand(
                    TrackedVehicleRuntime.DriveCommand(throttle = 1f, powered = true),
                )
            }
            .thenWaitUntil {
                val z = chassis.getPhysicsLocation(null).z.toDouble()
                val forward = chassis.getLinearVelocity(null).z.toDouble()
                helper.assertTrue(z > startZ.get() + 0.3, "waiting until the hull is actually moving forward, z=$z")
                drivenZ.set(z)
                drivenSpeed.set(forward)
            }
            .thenExecute {
                runtime.get()?.setDriveCommand(
                    TrackedVehicleRuntime.DriveCommand(throttle = 1f, brake = true, powered = true),
                )
            }
            .thenIdle(80)
            .thenExecute {
                val forward = chassis.getLinearVelocity(null).z.toDouble()
                helper.assertTrue(
                    forward < drivenSpeed.get() - 0.02,
                    "braking must cut the forward speed, was ${drivenSpeed.get()} now $forward",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 brake cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /** A right steer must decrease extracted physics yaw. Entity yaw is the negation, so this is a right turn. */
    @JvmStatic
    @GameTest(template = "empty", batch = "solo-right-steer", timeoutTicks = 900)
    fun steeringYawsTheHull(helper: GameTestHelper) {
        val (physics, chassis, runtime, _, setup) = movingHull(helper, "s4 yaw")
        val startYaw = java.util.concurrent.atomic.AtomicReference(0.0)
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "yaw setup did not run")
                setup.join().let { }
            }
            .thenExecute { runtime.get()?.create() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(30)
            .thenExecute {
                startYaw.set(yawOf(chassis.getPhysicsRotation(null)))
                runtime.get()?.setDriveCommand(
                    TrackedVehicleRuntime.DriveCommand(throttle = 0.4f, steer = 1f, powered = true),
                )
            }
            .thenIdle(100)
            .thenExecute {
                val delta = yawOf(chassis.getPhysicsRotation(null)) - startYaw.get()
                helper.assertTrue(
                    delta < -0.1,
                    "a right steer must decrease extracted physics yaw, delta=$delta rad",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 yaw cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * S3.1: a slab floor must hold the belt. The hull cannot collide with
     * terrain, so a fall means the slab shape was not supporting the runs.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun theBeltRestsOnASlab(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 6, 8, Blocks.STONE_SLAB)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 2.5f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s3 slab setup") {
            chassis.owner = host
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.isKinematic = true
            chassis.setPhysicsLocation(origin)
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "slab setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                runtime.set(created)
                created.contactDriveEnabled = false
                created.create()
            }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(40)
            .thenExecute { physics.submitTask("s3 slab release") { chassis.isKinematic = false } }
            .thenIdle(80)
            .thenExecute {
                val y = chassis.getPhysicsLocation(null).y.toDouble()
                helper.assertTrue(y > origin.y - 2.0, "the belt fell through the slab, y=$y start=${origin.y}")
                helper.assertTrue(runtime.get()?.beltSupported() == true || y > origin.y - 1.2, "the belt should be supported by the slab, y=$y")
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s3 slab cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /** Stairs must support the belt. The hull itself does not collide. */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun theBeltRestsOnStairs(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 6, 8, Blocks.STONE_STAIRS)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 2.5f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s3 stair setup") {
            chassis.owner = host
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "stair setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.contactDriveEnabled = false
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(80)
            .thenExecute {
                val y = chassis.getPhysicsLocation(null).y.toDouble()
                helper.assertTrue(y > origin.y - 2.0, "the belt fell through the stairs, y=$y start=${origin.y}")
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s3 stair cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * A two-block wall ahead must stop the whole vehicle. The chassis collides
     * with terrain here, so this is a vehicle-AABB stop, not a belt-only proof.
     * Step assist is off, so the hull must not climb the wall.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun aTwoBlockWallStopsTheHull(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 5, 8)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 10.0f)
        val wallZ = anchor.z + 6
        for (x in -4..4) {
            helper.getLevel().setBlock(BlockPos(anchor.x + x, anchor.y + 1, wallZ), Blocks.STONE.defaultBlockState(), 3)
            helper.getLevel().setBlock(BlockPos(anchor.x + x, anchor.y + 2, wallZ), Blocks.STONE.defaultBlockState(), 3)
        }
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s3 wall setup") {
            chassis.owner = host
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(com.atsuishio.superbwarfare.physics.tracked.TrackedTerrainCollision.TERRAIN_MESH)
            chassis.collideWithOwnerGroups =
                com.atsuishio.superbwarfare.physics.tracked.TrackedTerrainCollision.TERRAIN_MESH
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "wall setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.contactDriveEnabled = true
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(20)
            .thenExecute {
                runtime.get()?.setDriveCommand(TrackedVehicleRuntime.DriveCommand(throttle = 1f, powered = true))
            }
            .thenIdle(100)
            .thenExecute {
                val z = chassis.getPhysicsLocation(null).z.toDouble()
                helper.assertTrue(
                    z > wallZ + 0.05,
                    "the hull must stay on its side of the two-block wall at $wallZ, ended at $z",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s3 wall cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * Dig a pit in this structure's own floor after the stone is placed.
     * The belt must fall. Radius stays inside the structure so a neighbour's
     * floor cannot fill the pit.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun theBeltFallsThroughAHole(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -6..6) {
            for (z in -6..6) {
                for (y in 0..6) {
                    helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
                }
            }
        }
        val spec = compile()
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s3 hole setup") {
            chassis.owner = host
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.isKinematic = true
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "hole setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.contactDriveEnabled = false
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(80)
            .thenExecute { physics.submitTask("s3 hole release") { chassis.isKinematic = false } }
            .thenWaitUntil {
                val y = chassis.getPhysicsLocation(null).y.toDouble()
                helper.assertTrue(y < origin.y - 1.2, "a hole must not hold the belt, y=$y start=${origin.y}")
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s3 hole cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * S3.2 positive gate: one real block step must be crossed through the
     * generated Bullet ramp mesh. There is no contact-motion assist and no raw
     * AABB terrain contact in this path.
     *
     * The nose is chassis `+Z`. The step has to be at greater Z than the hull;
     * the previous layout put it behind the vehicle and then required the hull
     * to move toward `-Z`.
     */
    @JvmStatic
    @GameTest(template = "empty", batch = "solo-ramp-step", timeoutTicks = 1200)
    fun aOneBlockStepClimbsOnTheBulletRamp(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 5, 13)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 1.0f)
        val stepNearZ = anchor.z + 8
        val stepFarZ = anchor.z + 11
        for (x in -4..4) {
            for (z in stepNearZ until stepFarZ) {
                helper.getLevel().setBlock(BlockPos(anchor.x + x, anchor.y + 1, z), Blocks.STONE.defaultBlockState(), 3)
            }
        }
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val startY = java.util.concurrent.atomic.AtomicReference(Double.NaN)
        val peakY = java.util.concurrent.atomic.AtomicReference(Double.NEGATIVE_INFINITY)
        val sampledY = java.util.concurrent.atomic.AtomicReference(origin.y.toDouble())
        val sampledZ = java.util.concurrent.atomic.AtomicReference(origin.z.toDouble())
        val drive = java.util.concurrent.atomic.AtomicBoolean(false)
        val setup = physics.submitTask("s3 ramp step setup") {
            chassis.owner = host
            chassis.collisionGroup = CollisionGroups.CHASSIS
            // The belt must own the contact: the hull itself stays out of the
            // terrain query so this proves the local mesh carries the tracks.
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            physics.addBody(chassis)
            chassis.onPrePhysicsTick {
                val created = runtime.get()
                if (created != null && created.isCreated) {
                    created.applySuspension()
                    if (drive.get()) {
                        created.setDriveCommand(TrackedVehicleRuntime.DriveCommand(throttle = 1f, powered = true))
                        created.applyDrive()
                    }
                }
            }
            chassis.onPostPhysicsTick {
                val location = chassis.getPhysicsLocation(null)
                sampledY.set(location.y.toDouble())
                sampledZ.set(location.z.toDouble())
                peakY.accumulateAndGet(location.y.toDouble()) { left, right -> maxOf(left, right) }
            }
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "step setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.contactDriveEnabled = true
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil {
                val created = runtime.get() ?: error("no runtime")
                helper.assertTrue(created.isCreated, "the runtime must assemble")
                helper.assertTrue(created.terrainReady(), "the local Bullet terrain mesh must be ready: ${created.terrainDiagnostics()}")
            }
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                helper.assertTrue(
                    created.terrainRampCount() > 0,
                    "the one-block rise must produce a Bullet ramp: ${created.terrainDiagnostics()}",
                )
                startY.set(sampledY.get())
                drive.set(true)
            }
            .thenWaitUntil {
                helper.assertTrue(
                    peakY.get() - startY.get() >= .75,
                    "waiting to climb the Bullet ramp, y=${sampledY.get()} start=${startY.get()} peak=${peakY.get()}",
                )
            }
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                val z = sampledZ.get()
                helper.assertTrue(
                    z > stepNearZ + .25,
                    "the hull must reach the raised platform toward +Z, z=$z face=$stepNearZ y=${sampledY.get()} " +
                        created.terrainDiagnostics(),
                )
                helper.assertTrue(created.terrainTriangleCount() > 0, "the terrain mesh must contain triangles")
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s3 step cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * A stepped stone wedge must hold the belt.
     *
     * Minecraft has no smooth slope, so this is the real AABB ramp: one block
     * lower every four blocks of travel. The support wheels sit about 1.1 m
     * under the chassis origin and span roughly z -2.8..+2.9, so the hull is
     * spawned two blocks above the highest step under that patch. Spawning
     * inside the wedge lets the runs start penetrated and fall through.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun theBeltRestsOnARamp(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -5..5) {
            for (z in -6..6) {
                for (y in 0..10) {
                    helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
                }
            }
        }
        for (x in -5..5) {
            for (z in -6..6) {
                val top = rampTop(z)
                for (y in 0 until top) {
                    helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3)
                }
            }
        }
        val spec = compile()
        val surface = (CONTACT_Z_MIN..CONTACT_Z_MAX).maxOf { rampTop(it) }
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + surface + 2.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s3 ramp setup") {
            chassis.owner = host
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.isKinematic = true
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "ramp setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.contactDriveEnabled = false
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(80)
            .thenExecute { physics.submitTask("s3 ramp release") { chassis.isKinematic = false } }
            .thenIdle(40)
            .thenExecute {
                val y = chassis.getPhysicsLocation(null).y.toDouble()
                helper.assertTrue(
                    y > origin.y - 2.0,
                    "the belt fell through the ramp, y=$y start=${origin.y} surface=$surface",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s3 ramp cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * A deck with nothing under it. The hull does not collide with terrain, so
     * staying up means the belt is on the deck.
     *
     * Spawn is two blocks above the deck top, the same clearance the flat-floor
     * tests use. Closer than that starts the runs inside the deck.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun theBeltStaysOnABridgeDeck(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -5..5) {
            for (z in -6..6) {
                for (y in 0..8) {
                    helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
                }
                helper.getLevel().setBlock(anchor.offset(x, 4, z), Blocks.STONE.defaultBlockState(), 3)
            }
        }
        val spec = compile()
        val deckTop = anchor.y + 5.0
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 7.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s3 bridge setup") {
            chassis.owner = host
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.isKinematic = true
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "bridge setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.contactDriveEnabled = false
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(80)
            .thenExecute { physics.submitTask("s3 bridge release") { chassis.isKinematic = false } }
            .thenIdle(40)
            .thenExecute {
                val y = chassis.getPhysicsLocation(null).y.toDouble()
                helper.assertTrue(y > deckTop + 0.4, "the belt fell through the deck, y=$y deckTop=$deckTop")
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s3 bridge cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /** Forward contact must publish a finite per-side slip, not only a target sign. */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun forwardDrivePublishesFiniteSlip(helper: GameTestHelper) {
        val (physics, chassis, runtime, _, setup) = movingHull(helper, "s4 slip")
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "slip setup did not run")
                setup.join().let { }
            }
            .thenExecute { runtime.get()?.create() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(30)
            .thenExecute {
                runtime.get()?.setDriveCommand(TrackedVehicleRuntime.DriveCommand(throttle = 1f, powered = true))
            }
            .thenIdle(80)
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                val left = created.lastSideContact(TrackedSide.LEFT)
                val right = created.lastSideContact(TrackedSide.RIGHT)
                helper.assertTrue(left.contactCount + right.contactCount > 0, "a driven belt must sample terrain, left=$left right=$right")
                val sample = if (left.contactCount >= right.contactCount) left else right
                helper.assertTrue(sample.targetSpeedMps > 1.0, "full throttle must publish a forward target, was ${sample.targetSpeedMps}")
                helper.assertTrue(sample.normalImpulseNs > 0.0, "the slip sample must come from a real terrain impulse, was $sample")
                helper.assertTrue(sample.slipMps.isFinite() && sample.tangentialSpeedMps.isFinite(), "slip must be a real measurement, was $sample")
                helper.assertTrue(
                    abs(sample.slipMps - (sample.tangentialSpeedMps - sample.targetSpeedMps)) < 1.0e-6,
                    "slip must be tangential speed minus target, was $sample",
                )
                helper.assertTrue(kotlin.math.abs(sample.slipMps) < 40.0, "slip is not a finite contact reading: $sample")
                listOf(left, right).filter { it.contactCount > 0 }.forEach { side ->
                    helper.assertTrue(
                        abs(side.slipMps - (side.tangentialSpeedMps - side.targetSpeedMps)) < 1.0e-6,
                        "each contacting side must own its own slip, left=$left right=$right",
                    )
                }
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 slip cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * Coast must not keep driving the belt.
     *
     * The hull starts on the floor. [movingHull] spawns past the floor lip, and a
     * lip collision reverses chassis velocity, which is not a coast measurement.
     *
     * `RollingResistance` is *not* implemented: two velocity-target
     * implementations were measured and both added energy (one braked hard, one
     * accelerated the hull after release). This case therefore asserts only the
     * property that is true today: releasing the throttle removes the conveyor
     * target instead of leaving a stale one that keeps pushing.
     */
    @JvmStatic
    @GameTest(template = "empty", batch = "solo-coast", timeoutTicks = 900)
    fun coastRemovesTheConveyorTargetInsteadOfDriving(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 6, 10)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val startZ = java.util.concurrent.atomic.AtomicReference(origin.z.toDouble())
        val coastZ = java.util.concurrent.atomic.AtomicReference(0.0)
        val drivenSpeed = java.util.concurrent.atomic.AtomicReference(0.0)
        val setup = physics.submitTask("s4 coast setup") {
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.owner = host
            physics.addBody(chassis)
            startZ.set(origin.z.toDouble())
        }
        val createdRuntime = TrackedVehicleRuntime(physics, host, spec, chassis)
        createdRuntime.contactDriveEnabled = true
        runtime.set(createdRuntime)
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "coast setup did not run")
                setup.join().let { }
            }
            .thenExecute { runtime.get()?.create() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(20)
            .thenExecute {
                runtime.get()?.setDriveCommand(TrackedVehicleRuntime.DriveCommand(throttle = 1f, powered = true))
            }
            .thenWaitUntil {
                val z = chassis.getPhysicsLocation(null).z.toDouble()
                val forward = chassis.getLinearVelocity(null).z.toDouble()
                helper.assertTrue(
                    z > startZ.get() + 0.4 && forward > 0.6,
                    "waiting until the hull is moving on the floor, z=$z speed=$forward",
                )
                coastZ.set(z)
                drivenSpeed.set(forward)
            }
            .thenExecute {
                runtime.get()?.setDriveCommand(TrackedVehicleRuntime.DriveCommand())
            }
            // Short window: the floor ends at z=-10 from the spawn, and a longer
            // coast drives the hull off the lip, where the measured speed is no
            // longer a free roll.
            .thenIdle(40)
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                val z = chassis.getPhysicsLocation(null).z.toDouble()
                val forward = chassis.getLinearVelocity(null).z.toDouble()
                helper.assertTrue(created.lastDemand().state == com.atsuishio.superbwarfare.physics.tracked.TrackedDriveController.DriveState.COAST, "released throttle must be COAST")
                // Releasing the throttle must remove the conveyor target, not
                // leave a stale one that keeps propelling the belt.
                helper.assertTrue(
                    created.lastContactTarget() == 0f,
                    "coast must not present a drive target, was ${created.lastContactTarget()}",
                )
                helper.assertTrue(
                    created.lastCoastTarget() == 0f,
                    "coast must not write a velocity target; RollingResistance is not implemented, " +
                        "was ${created.lastCoastTarget()}",
                )
                helper.assertTrue(
                    z < coastZ.get() - 0.2,
                    "coast must keep moving in the drive direction, coastZ=${coastZ.get()} now $z " +
                        "speed was ${drivenSpeed.get()} now $forward",
                )
                // The row must not act as a throttle: a coasting vehicle may
                // slow from friction but must not gain speed like a driven one.
                val gain = forward - drivenSpeed.get()
                val gainFraction = if (drivenSpeed.get() > 0.0) gain / drivenSpeed.get() else 0.0
                Mod.LOGGER.info(
                    "[s4-coast] ticks=40 from={} to={} gain={} fraction={}",
                    drivenSpeed.get(), forward, gain, gainFraction,
                )
                helper.assertTrue(
                    gainFraction < 0.5,
                    "coast must not keep accelerating like a driven belt, was ${drivenSpeed.get()} now $forward " +
                        "fraction=$gainFraction",
                )
                helper.assertTrue(
                    forward > 0.0,
                    "the hull must still be rolling forward in the measured window, was ${drivenSpeed.get()} now $forward",
                )
                // ...but it must not be an immediate stop.
                helper.assertTrue(
                    forward > drivenSpeed.get() * 0.2,
                    "coast must not be an immediate stop, was ${drivenSpeed.get()} now $forward",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 coast cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /** A longer drive must stay finite and supported. This is not a soak approval. */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 1400)
    fun aLongDriveStaysFiniteAndSupported(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 6, 10)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val startZ = java.util.concurrent.atomic.AtomicReference(origin.z.toDouble())
        val setup = physics.submitTask("s4 long setup") {
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.owner = host
            physics.addBody(chassis)
            startZ.set(origin.z.toDouble())
        }
        val created = TrackedVehicleRuntime(physics, host, spec, chassis)
        created.contactDriveEnabled = true
        runtime.set(created)
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "long-drive setup did not run")
                setup.join().let { }
            }
            .thenExecute { runtime.get()?.create() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(20)
            .thenExecute {
                runtime.get()?.setDriveCommand(TrackedVehicleRuntime.DriveCommand(throttle = 0.6f, powered = true))
            }
            .thenIdle(600)
            .thenExecute {
                val location = chassis.getPhysicsLocation(null)
                val velocity = chassis.getLinearVelocity(null)
                helper.assertTrue(
                    location.x.isFinite() && location.y.isFinite() && location.z.isFinite() &&
                        velocity.x.isFinite() && velocity.y.isFinite() && velocity.z.isFinite(),
                    "a long drive must stay finite, pos=$location vel=$velocity",
                )
                helper.assertTrue(location.y > -64.0, "the hull fell out of the world, y=${location.y}")
                val angular = chassis.getAngularVelocity(null)
                helper.assertTrue(
                    angular.x.isFinite() && angular.y.isFinite() && angular.z.isFinite() &&
                        kotlin.math.abs(angular.y) < 3.0,
                    "a 30s drive must not spin up, angular=$angular",
                )
                helper.assertTrue(location.y > origin.y - 5.0, "the hull fell through the floor, y=${location.y} start=${origin.y}")
                helper.assertTrue(location.z > startZ.get() + 0.5, "partial throttle must still make progress, z=${location.z}")
                helper.assertTrue((runtime.get()?.contactMotionCount() ?: 0) > 0, "the long drive must keep writing contact targets")
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 long cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * Only the left belt has a pad. The chassis is kinematic so roll cannot move
     * the right belt onto that pad. This is the single-side load split.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun oneSideCarriesTheTerrainLoad(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -6..6) for (z in -6..6) for (y in 0..6) {
            helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
        }
        val spec = compile()
        val leftX = spec.side(TrackedSide.LEFT).wheels.map { it.assemblyCenter.x }.average()
        val rightX = spec.side(TrackedSide.RIGHT).wheels.map { it.assemblyCenter.x }.average()
        helper.assertTrue(kotlin.math.abs(leftX - rightX) > 1.2, "the tracks must be far enough apart to isolate, left=$leftX right=$rightX")
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.2f, anchor.z + 0.5f)
        val padX = kotlin.math.floor(origin.x + leftX).toInt()
        for (z in -4..4) {
            helper.getLevel().setBlock(BlockPos(padX, anchor.y, z + anchor.z), Blocks.STONE.defaultBlockState(), 3)
            helper.getLevel().setBlock(BlockPos(padX, anchor.y + 1, z + anchor.z), Blocks.STONE.defaultBlockState(), 3)
        }
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s4 oneside setup") {
            chassis.isKinematic = true
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.owner = host
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "oneside setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.contactDriveEnabled = false
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(40)
            .thenExecute {
                physics.submitTask("s4 oneside lower") {
                    val location = chassis.getPhysicsLocation(null)
                    chassis.setPhysicsLocation(location.add(0f, -0.8f, 0f))
                }
            }
            .thenIdle(40)
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                val left = created.lastSideContact(TrackedSide.LEFT)
                val right = created.lastSideContact(TrackedSide.RIGHT)
                helper.assertTrue(
                    left.normalImpulseNs > 0.5,
                    "the padded side must carry load, left=$left right=$right padX=$padX ${created.terrainDiagnostics()}",
                )
                helper.assertTrue(
                    right.normalImpulseNs < left.normalImpulseNs * 0.25,
                    "the open side must not share that load, left=$left right=$right ${created.terrainDiagnostics()}",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 oneside cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /** Air under a deck must not hold the belt, and the deck must not pull it upward. */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun theBeltFallsUnderABridgeDeck(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -5..5) for (z in -6..6) for (y in 0..8) {
            helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
        }
        for (x in -5..5) for (z in -6..6) {
            helper.getLevel().setBlock(anchor.offset(x, 5, z), Blocks.STONE.defaultBlockState(), 3)
        }
        val spec = compile()
        // Spawn in the air gap under the deck, clear of both the deck underside
        // and any cell floor the structure may still have at y=0.
        val deckBottom = anchor.y + 5.0
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.2f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s3 underbridge setup") {
            chassis.owner = host
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.isKinematic = true
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "under-bridge setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.contactDriveEnabled = false
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(80)
            .thenExecute { physics.submitTask("s3 underbridge release") { chassis.isKinematic = false } }
            .thenWaitUntil {
                val y = chassis.getPhysicsLocation(null).y.toDouble()
                val created = runtime.get() ?: error("no runtime")
                helper.assertTrue(
                    y < origin.y - 0.6,
                    "the deck must not hold a belt spawned under it, y=$y start=${origin.y} ${created.terrainDiagnostics()}",
                )
                helper.assertTrue(
                    y < deckBottom - 1.0,
                    "the belt must stay under the deck, y=$y deckBottom=$deckBottom ${created.terrainDiagnostics()}",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s3 underbridge cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * Same pad split, but the hull is dynamic. Vertical motion stays free so the
     * belt can settle under gravity. Horizontal motion and rotation are locked
     * so roll cannot drag the open side onto the pad. This is not a free tip-over.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun aDynamicHullLoadsOnlyThePaddedSide(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -6..6) for (z in -6..6) for (y in 0..6) {
            helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
        }
        val spec = compile()
        val leftX = spec.side(TrackedSide.LEFT).wheels.map { it.assemblyCenter.x }.average()
        val rightX = spec.side(TrackedSide.RIGHT).wheels.map { it.assemblyCenter.x }.average()
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.2f, anchor.z + 0.5f)
        val padX = kotlin.math.floor(origin.x + leftX).toInt()
        for (z in -4..4) {
            helper.getLevel().setBlock(BlockPos(padX, anchor.y, z + anchor.z), Blocks.STONE.defaultBlockState(), 3)
            helper.getLevel().setBlock(BlockPos(padX, anchor.y + 1, z + anchor.z), Blocks.STONE.defaultBlockState(), 3)
        }
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s4 dynamic oneside setup") {
            chassis.setAngularFactor(Vector3f(0f, 0f, 0f))
            chassis.setLinearFactor(Vector3f(0f, 1f, 0f))
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            chassis.owner = host
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "dynamic oneside setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.contactDriveEnabled = false
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(80)
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                val left = created.lastSideContact(TrackedSide.LEFT)
                val right = created.lastSideContact(TrackedSide.RIGHT)
                helper.assertTrue(left.normalImpulseNs > 0.5, "the padded side must carry the dynamic hull, left=$left right=$right")
                helper.assertTrue(
                    right.normalImpulseNs < left.normalImpulseNs * 0.25,
                    "the open side must not share that dynamic load, left=$left right=$right",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 dynamic oneside cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /** A belt resting on the floor under a deck must drive forward and stay under the deck. */
    @JvmStatic
    @GameTest(template = "empty", batch = "solo-under-deck-drive", timeoutTicks = 900)
    fun theBeltDrivesUnderABridgeDeck(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -5..5) for (z in -8..8) for (y in 0..8) {
            helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
        }
        for (x in -5..5) for (z in -8..8) {
            helper.getLevel().setBlock(anchor.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3)
            helper.getLevel().setBlock(anchor.offset(x, 6, z), Blocks.STONE.defaultBlockState(), 3)
        }
        val spec = compile()
        val deckBottom = anchor.y + 6.0
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val startZ = java.util.concurrent.atomic.AtomicReference(origin.z.toDouble())
        val setup = physics.submitTask("s3 underdrive setup") {
            chassis.owner = host
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            physics.addBody(chassis)
            startZ.set(origin.z.toDouble())
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "under-deck drive setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.contactDriveEnabled = true
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(40)
            .thenExecute {
                runtime.get()?.setDriveCommand(TrackedVehicleRuntime.DriveCommand(throttle = 1f, powered = true))
            }
            .thenIdle(80)
            .thenExecute {
                val location = chassis.getPhysicsLocation(null)
                helper.assertTrue(
                    location.y.toDouble() < deckBottom - 0.5,
                    "driving under a deck must not climb onto it, y=${location.y} deckBottom=$deckBottom",
                )
                helper.assertTrue(
                    location.z.toDouble() > startZ.get() + 0.25,
                    "the belt must still drive under the deck, start ${startZ.get()} now ${location.z}",
                )
                helper.assertTrue((runtime.get()?.contactMotionCount() ?: 0) > 0, "under-deck drive must write contact targets")
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s3 underdrive cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /** A left steer must yaw the opposite way from a right steer. */
    @JvmStatic
    @GameTest(template = "empty", batch = "solo-left-steer", timeoutTicks = 900)
    fun aLeftSteerYawsTheOtherWay(helper: GameTestHelper) {
        val (physics, chassis, runtime, _, setup) = movingHull(helper, "s4 yaw left")
        val startYaw = java.util.concurrent.atomic.AtomicReference(0.0)
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "left-yaw setup did not run")
                setup.join().let { }
            }
            .thenExecute { runtime.get()?.create() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(30)
            .thenExecute {
                startYaw.set(yawOf(chassis.getPhysicsRotation(null)))
                runtime.get()?.setDriveCommand(
                    TrackedVehicleRuntime.DriveCommand(throttle = 0.4f, steer = -1f, powered = true),
                )
            }
            .thenIdle(100)
            .thenExecute {
                val delta = yawOf(chassis.getPhysicsRotation(null)) - startYaw.get()
                helper.assertTrue(delta > 0.1, "a left steer must increase extracted physics yaw, delta=$delta rad")
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 yaw left cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * Steady slip must stay finite and inside the controller's authored speed
     * plus 1 m/s. That band is a health check, not a frozen acceptance limit.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun steadySlipStaysInsideTheSpeedBand(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 6, 10)
        val spec = compile()
        val band = spec.drivetrain.maxSpeedMps + 1.0
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s4 slipband setup") {
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.owner = host
            physics.addBody(chassis)
        }
        val createdRuntime = TrackedVehicleRuntime(physics, host, spec, chassis)
        createdRuntime.contactDriveEnabled = true
        runtime.set(createdRuntime)
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "slip-band setup did not run")
                setup.join().let { }
            }
            .thenExecute { runtime.get()?.create() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(30)
            .thenExecute {
                runtime.get()?.setDriveCommand(TrackedVehicleRuntime.DriveCommand(throttle = 1f, powered = true))
            }
            .thenIdle(100)
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                val sides = listOf(TrackedSide.LEFT, TrackedSide.RIGHT).map { created.lastSideContact(it) }
                helper.assertTrue(sides.sumOf { it.contactCount } > 0, "steady drive must keep a terrain contact, $sides")
                sides.filter { it.contactCount > 0 }.forEach { sample ->
                    helper.assertTrue(
                        abs(sample.slipMps - (sample.tangentialSpeedMps - sample.targetSpeedMps)) < 1.0e-6,
                        "slip identity failed: $sample",
                    )
                    helper.assertTrue(
                        abs(sample.slipMps) <= band && abs(sample.tangentialSpeedMps) <= band && abs(sample.targetSpeedMps) <= band,
                        "slip left the authored speed band $band: $sample",
                    )
                }
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 slipband cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /** A wall in front of one belt must raise that side's slip and still report load. */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun oneSideAgainstAWallShowsMoreSlip(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 6, 10)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 2.0f)
        val leftX = spec.side(TrackedSide.LEFT).wheels.map { it.assemblyCenter.x }.average()
        val wallX = kotlin.math.floor(origin.x + leftX).toInt()
        val wallZ = anchor.z - 4
        for (y in 1..2) {
            helper.getLevel().setBlock(BlockPos(wallX, anchor.y + y, wallZ), Blocks.STONE.defaultBlockState(), 3)
        }
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s4 wallslip setup") {
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.owner = host
            physics.addBody(chassis)
        }
        val createdRuntime = TrackedVehicleRuntime(physics, host, spec, chassis)
        createdRuntime.contactDriveEnabled = true
        runtime.set(createdRuntime)
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "wall-slip setup did not run")
                setup.join().let { }
            }
            .thenExecute { runtime.get()?.create() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(30)
            .thenExecute {
                runtime.get()?.setDriveCommand(TrackedVehicleRuntime.DriveCommand(throttle = 1f, powered = true))
            }
            .thenWaitUntil {
                val created = runtime.get() ?: error("no runtime")
                val left = created.lastSideContact(TrackedSide.LEFT)
                val right = created.lastSideContact(TrackedSide.RIGHT)
                helper.assertTrue(
                    left.contactCount > 0 && right.contactCount > 0 && left.normalImpulseNs > 0.0,
                    "waiting until both belts are loaded against the one-side wall, left=$left right=$right",
                )
                helper.assertTrue(
                    abs(left.slipMps) > abs(right.slipMps) + 0.05,
                    "the blocked side must slip more, left=$left right=$right",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 wallslip cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * Free hull, no rotation lock. The open side has no pad, so a load split
     * must appear before the hull tips off the padded side.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun aFreeHullLoadsThePaddedSideBeforeItTips(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -6..6) for (z in -6..6) for (y in 0..6) {
            helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
        }
        val spec = compile()
        val leftX = spec.side(TrackedSide.LEFT).wheels.map { it.assemblyCenter.x }.average()
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.2f, anchor.z + 0.5f)
        val padX = kotlin.math.floor(origin.x + leftX).toInt()
        for (z in -4..4) for (dx in 0..1) {
            helper.getLevel().setBlock(BlockPos(padX + dx, anchor.y, z + anchor.z), Blocks.STONE.defaultBlockState(), 3)
            helper.getLevel().setBlock(BlockPos(padX + dx, anchor.y + 1, z + anchor.z), Blocks.STONE.defaultBlockState(), 3)
        }
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s4 free oneside setup") {
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.setPhysicsLocation(origin)
            chassis.owner = host
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "free oneside setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.contactDriveEnabled = false
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenWaitUntil {
                val created = runtime.get() ?: error("no runtime")
                val left = created.lastSideContact(TrackedSide.LEFT)
                val right = created.lastSideContact(TrackedSide.RIGHT)
                helper.assertTrue(
                    left.normalImpulseNs > 0.5 && right.normalImpulseNs < left.normalImpulseNs * 0.25 && left.slipMps.isFinite(),
                    "waiting for a free-hull load split before tip-over, left=$left right=$right",
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 free oneside cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /** Block-top Y of the stepped ramp at a structure-local z. */
    private fun rampTop(z: Int): Int = 5 + (2 - z) / 4

    private fun yawOf(rotation: com.jme3.math.Quaternion): Double {
        val siny = 2.0 * (rotation.w * rotation.y + rotation.z * rotation.x)
        val cosy = 1.0 - 2.0 * (rotation.y * rotation.y + rotation.x * rotation.x)
        return kotlin.math.atan2(siny, cosy)
    }

    /**
     * S4 slip band on steady throttle.
     *
     * `maxSpeed + 1 m/s` was only a health check. This measures the actual
     * distribution: every driven belt contact on both sides must stay inside a
     * declared band, and the band must be finite and non-degenerate. A vehicle
     * that is not touching the ground has no samples, which this test rejects
     * instead of silently passing.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 1200)
    fun steadyThrottleKeepsSlipInsideADeclaredBand(helper: GameTestHelper) {
        val (physics, chassis, runtime, _, setup) = movingHull(helper, "s4 slip band")
        val leftBand = java.util.concurrent.atomic.AtomicReference(
            TrackedVehicleRuntime.SlipBand.EMPTY,
        )
        val rightBand = java.util.concurrent.atomic.AtomicReference(
            TrackedVehicleRuntime.SlipBand.EMPTY,
        )
        val target = java.util.concurrent.atomic.AtomicReference(0.0)
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "slip band setup did not run")
                setup.join().let { }
            }
            .thenExecute { runtime.get()?.create() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(40)
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                created.resetSlipBand()
                created.setDriveCommand(TrackedVehicleRuntime.DriveCommand(throttle = 1f, powered = true))
            }
            .thenWaitUntil {
                val created = runtime.get() ?: error("no runtime")
                val left = created.slipBand(TrackedSide.LEFT)
                val right = created.slipBand(TrackedSide.RIGHT)
                helper.assertTrue(
                    left.samples > 40 && right.samples > 40,
                    "waiting for a steady driven slip sample, left=${left.samples} right=${right.samples}",
                )
                leftBand.set(left)
                rightBand.set(right)
                target.set(created.lastDemand().targetSpeedMps)
            }
            .thenExecute {
                // Use the target observed *while sampling*, not the target at
                // the end of the run: a vehicle that ran out of energy has
                // already decayed to COAST, so an end-of-run target would be 0
                // and would make the band look violated.
                for ((name, band) in listOf("left" to leftBand.get(), "right" to rightBand.get())) {
                    val limit = band.maxTargetMps + 1.0
                    helper.assertTrue(
                        band.maxMps.isFinite() && band.minMps.isFinite(),
                        "$name slip band must be finite, $band",
                    )
                    helper.assertTrue(
                        band.maxMps <= limit,
                        "$name slip exceeded the declared band: max=${band.maxMps} limit=$limit band=$band",
                    )
                    helper.assertTrue(
                        band.maxMps - band.minMps > 1.0e-6,
                        "$name slip band is degenerate, which means nothing was actually measured: $band",
                    )
                }
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 slip cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * Long soak: 1800 ticks of partial throttle on real terrain.
     *
     * This is not a stability proof, but it does reject the failure modes a
     * short test hides: NaN/Inf state, sinking through the floor, losing all
     * contact, or the slip band drifting open over time.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 3000)
    fun aLongPartialThrottleSoakStaysFiniteAndSupported(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 8, 20)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 3.0f, anchor.z + 18.0f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), spec.sprungAssemblyMassKg.toFloat())
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val startZ = java.util.concurrent.atomic.AtomicReference(origin.z.toDouble())
        val setup = physics.submitTask("s4 soak setup") {
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.CHASSIS
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.setSleepingThresholds(0f, 0f)
            chassis.owner = host
            physics.addBody(chassis)
            startZ.set(origin.z.toDouble())
        }
        val createdRuntime = TrackedVehicleRuntime(physics, host, spec, chassis)
        createdRuntime.contactDriveEnabled = true
        runtime.set(createdRuntime)
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "soak setup did not run")
                setup.join().let { }
            }
            .thenExecute { runtime.get()?.create() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must assemble") }
            .thenIdle(40)
            .thenExecute {
                runtime.get()?.resetSlipBand()
                runtime.get()?.setDriveCommand(
                    TrackedVehicleRuntime.DriveCommand(throttle = 0.35f, powered = true),
                )
            }
            .thenIdle(1800)
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                val location = chassis.getPhysicsLocation(null)
                val velocity = chassis.getLinearVelocity(null)
                helper.assertTrue(
                    location.x.isFinite() && location.y.isFinite() && location.z.isFinite(),
                    "soak position must stay finite, got $location",
                )
                helper.assertTrue(
                    velocity.x.isFinite() && velocity.y.isFinite() && velocity.z.isFinite(),
                    "soak velocity must stay finite, got $velocity",
                )
                helper.assertTrue(
                    location.y.toDouble() > anchor.y - 2.0,
                    "the soaked hull must not sink through the floor, y=${location.y} floor=${anchor.y}",
                )
                helper.assertTrue(
                    location.z.toDouble() > startZ.get() + 1.0,
                    "1800 ticks of throttle must make real progress, start ${startZ.get()} now ${location.z}",
                )
                val left = created.slipBand(TrackedSide.LEFT)
                val right = created.slipBand(TrackedSide.RIGHT)
                helper.assertTrue(
                    left.samples > 200 && right.samples > 200,
                    "the soak must keep both belts in contact, left=${left.samples} right=${right.samples}",
                )
                // Measured, not assumed: sustained partial throttle on AABB
                // terrain produces a real, large negative mean slip (the belt
                // surface lags the command while the hull accelerates and
                // climbs block edges). This test therefore bounds the band by
                // an explicit physical limit instead of the +1 m/s health check,
                // and records the observed values so the number is auditable.
                val physicalLimit = created.lastDemand().targetSpeedMps + 20.0
                helper.assertTrue(
                    left.maxMps <= physicalLimit && right.maxMps <= physicalLimit,
                    "soak slip must stay finite and bounded, left=$left right=$right limit=$physicalLimit",
                )
                Mod.LOGGER.info(
                    "[s4-soak] ticks=1800 left={} right={} displacementZ={}",
                    left, right, location.z - startZ.get(),
                )
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil { helper.assertTrue(runtime.get()?.isClosed == true, "the runtime must close") }
            .thenExecute { physics.submitTask("s4 soak cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * S4 through the production input chain, not the runtime API.
     *
     * The other S4 cases call `TrackedVehicleRuntime.setDriveCommand` directly.
     * That proves the running gear reacts to a command, but it skips everything
     * that turns a driver into a command: mounting, `processInput`,
     * `VehicleEngineUtils.trackEngine`, `VehiclePhysics.control` and the
     * `trackedCommand` publish. A regression in any of those would leave every
     * other S4 test green.
     *
     * This test seats a GameTest mock player, drives the vehicle with the same
     * packet entry point the client uses (`serverInput`), and asserts the
     * resulting per-side command came out of the real entity path.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 1200)
    fun theRealVehicleInputPathPublishesATrackedDriveCommand(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper, 6, 10)
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val vehicle = ModEntities.ZTZ_99A.get().create(helper.getLevel())
        helper.assertTrue(vehicle != null, "ZTZ-99A entity could not be created")
        vehicle!!.setPos(anchor.x + 0.5, anchor.y + 4.0, anchor.z + 0.5)
        helper.getLevel().addFreshEntity(vehicle)

        val forwardKey = 0b00000100.toShort()
        val neutralKey = 0.toShort()
        val startZ = java.util.concurrent.atomic.AtomicReference(0.0)
        val seenPowered = java.util.concurrent.atomic.AtomicBoolean(false)
        val seenForwardZ = java.util.concurrent.atomic.AtomicReference(0.0)

        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(VehiclePhysics.hasBody(vehicle), "the 99A must assemble before input")
            }
            .thenExecute {
                // A vehicle with no energy refuses throttle: `trackEngine` clears
                // the inputs and decays power. That is production behaviour, so
                // the test has to provide the same precondition a real driver
                // needs instead of weakening the assertion.
                vehicle.energy = vehicle.maxEnergy
                helper.assertTrue(
                    vehicle.energy > 0,
                    "the 99A must have energy before the input path can drive, was ${vehicle.energy}",
                )
                val player = helper.makeMockPlayer()
                helper.assertTrue(player != null, "the GameTest mock player is required")
                player!!.setPos(vehicle.x, vehicle.y + 1.0, vehicle.z)
                player.startRiding(vehicle, true)
                helper.assertTrue(
                    vehicle.getFirstPassenger() === player,
                    "the mock player must occupy the first seat, got ${vehicle.getFirstPassenger()}",
                )
                // Real client packet path: VehicleMovementMessage -> serverInput.
                VehiclePhysics.serverInput(vehicle, forwardKey, 1L, player)
                startZ.set(vehicle.z)
            }
            .thenWaitUntil {
                // The entity engine must actually produce throttle. `power` is
                // only advanced by trackEngine when a passenger is present.
                val powered = vehicle.power > 0.05f
                if (powered) seenPowered.set(true)
                helper.assertTrue(powered, "throttle input must raise power, was ${vehicle.power}")
            }
            .thenWaitUntil {
                val runtime = VehiclePhysics.trackedRuntime(vehicle)
                    ?: error("the tracked runtime is missing")
                val command = VehiclePhysics.driveCommand(vehicle)
                helper.assertTrue(
                    command != null && command.powered,
                    "the real input path must publish a powered tracked command, got $command",
                )
                seenForwardZ.set(vehicle.z)
                helper.assertTrue(
                    command!!.throttle > 0.1f,
                    "throttle=1 must publish a positive tracked throttle, got ${command.throttle}",
                )
                helper.assertTrue(
                    runtime.contactMotionCount() > 0,
                    "the real input path must reach belt contact, count ${runtime.contactMotionCount()}",
                )
                // The first-chance hook must actually fire. If contactConceived
                // were not requested from Bullet, this stays zero and a fresh
                // contact would spend its first substep without a target.
                helper.assertTrue(
                    runtime.conceivedContactMotionCount() > 0,
                    "belt contact must be configured before the point enters the manifold, " +
                        "conceived=${runtime.conceivedContactMotionCount()} " +
                        "processed=${runtime.contactMotionCount()}",
                )
            }
            .thenWaitUntil {
                helper.assertTrue(
                    vehicle.z > startZ.get() + 0.25,
                    "the real input path must move the hull forward, start ${startZ.get()} now ${vehicle.z}",
                )
            }
            .thenExecute {
                val player = vehicle.getFirstPassenger() as? net.minecraft.world.entity.player.Player
                    ?: error("mock player left the seat")
                // Release throttle and let the engine wind down through the
                // same path; COAST must clear the contact target.
                VehiclePhysics.serverInput(vehicle, neutralKey, 2L, player)
                vehicle.stopRiding()
            }
            .thenWaitUntil {
                helper.assertTrue(seenPowered.get(), "the drive phase must have been powered")
                val runtime = VehiclePhysics.trackedRuntime(vehicle) ?: error("runtime disappeared")
                helper.assertTrue(
                    runtime.lastDemand().state == com.atsuishio.superbwarfare.physics.tracked.TrackedDriveController.DriveState.COAST,
                    "an empty seat must resolve to COAST, was ${runtime.lastDemand().state}",
                )
            }
            .thenExecute { vehicle.discard() }
            .thenWaitUntil {
                helper.assertFalse(VehiclePhysics.isManaged(vehicle), "the driven 99A must be released")
                val close = VehiclePhysics.closeFuture(vehicle)
                helper.assertTrue(close != null && close.isDone, "the driven 99A teardown must finish")
                helper.assertTrue(
                    close != null && !close.isCompletedExceptionally,
                    "the driven 99A teardown failed",
                )
            }
            .thenSucceed()
    }

}
