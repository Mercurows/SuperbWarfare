package com.atsuishio.superbwarfare.gametest

import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.SparkPhysics
import cn.solarmoon.spark_core.physics.body.CollisionGroups
import cn.solarmoon.spark_core.physics.body.owner
import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.physics.tracked.CompiledTrackSide
import com.atsuishio.superbwarfare.physics.tracked.TrackedRunSpecs
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
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * S2 gate: the production [TrackedVehicleRuntime], not a fixture.
 *
 * The B2 fixtures prove the mechanism semantics; this proves the **runtime**
 * composes the same pieces for a whole vehicle and can be torn down cleanly.
 * It uses both sides, so the count assertions cover the real vehicle scale:
 * 8 wheel bodies and 8 runs per side, 16 external end constraints per side.
 *
 * The chassis is static here on purpose. This test asks whether assembly and
 * teardown are correct, not whether the vehicle is held up; load carrying is
 * B2.3/B2.4, and a moving hull would make anchor gaps unmeasurable.
 */
@GameTestHolder(Mod.MODID)
@PrefixGameTestTemplate(false)
object TrackedVehicleRuntimeGameTests {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun resource(path: String): String {
        val stream = TrackedVehicleRuntimeGameTests::class.java.classLoader.getResourceAsStream(path)
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

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 800)
    fun theProductionRuntimeAssemblesAndTearsDownBothSides(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -4..4) {
            for (z in -5..5) {
                for (y in 1..8) {
                    helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
                }
            }
        }

        val spec = compile()
        val left: CompiledTrackSide = spec.side(TrackedSide.LEFT)
        val right: CompiledTrackSide = spec.side(TrackedSide.RIGHT)
        helper.assertTrue(
            TrackedRunSpecs.compileRuns(spec, TrackedSide.LEFT).size == left.runs.size,
            "left runs must match the compiled side",
        )

        val host = object : PhysicsHost {
            override val physicsLevel: PhysicsLevel get() = physics
            override val allPhysicsBodies: MutableMap<String, PhysicsCollisionObject> = ConcurrentHashMap()
        }

        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 5f, anchor.z + 0.5f)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), 0f)
        val closeFuture = java.util.concurrent.atomic.AtomicReference<CompletableFuture<Void>?>(null)
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)

        val bodyStage = physics.submitTask("s2 runtime chassis") {
            chassis.isKinematic = true
            chassis.setPhysicsLocation(origin)
            // Inert group: the runs must not be ejected from inside the hull
            // box, and this test is about assembly, not contact.
            chassis.collisionGroup = CollisionGroups.PHYSICS_BODY
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.owner = host
            physics.addBody(chassis)
        }

        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(bodyStage.isDone, "the chassis task did not run")
                bodyStage.join().let { }
                helper.assertTrue(chassis.isInWorld, "the chassis must be in the world")
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                runtime.set(created)
                closeFuture.set(created.create())
            }
            .thenWaitUntil {
                val created = runtime.get() ?: error("runtime was not created")
                helper.assertTrue(created.isCreated, "the runtime must report created")
                val expectedWheels = left.wheels.size + right.wheels.size
                val expectedRuns = left.runs.size + right.runs.size
                val expectedEnds = (left.runs.size + right.runs.size) * 2
                helper.assertTrue(
                    created.wheelCount == expectedWheels,
                    "expected $expectedWheels wheel bodies, got ${created.wheelCount}",
                )
                helper.assertTrue(
                    created.runCount == expectedRuns,
                    "expected $expectedRuns run runtimes, got ${created.runCount}",
                )
                helper.assertTrue(
                    created.endJointCount == expectedEnds,
                    "expected $expectedEnds external end joints, got ${created.endJointCount}",
                )
                val mounts = left.mounts.count { it.joint != null } + right.mounts.count { it.joint != null }
                helper.assertTrue(mounts > 0, "the compiled vehicle must declare wheel mounts")
            }
            // Teardown: the runtime must remove every object it created and
            // leave the caller-owned chassis alone.
            .thenExecute {
                runtime.get()?.let { closeFuture.set(it.closeAsync()) }
            }
            .thenWaitUntil {
                val future = closeFuture.get() ?: error("close was not started")
                helper.assertTrue(future.isDone, "closeAsync must complete")
                helper.assertTrue(
                    !future.isCompletedExceptionally,
                    "closeAsync must complete without exception: ${future.getNow(null)}",
                )
                val created = runtime.get() ?: error("runtime was not created")
                helper.assertTrue(created.isClosed, "the runtime must report closed")
                helper.assertTrue(created.wheelCount == 0, "wheel bodies must be released, got ${created.wheelCount}")
                helper.assertTrue(created.runCount == 0, "run runtimes must be released, got ${created.runCount}")
                helper.assertTrue(created.endJointCount == 0, "end joints must be released, got ${created.endJointCount}")
                helper.assertTrue(chassis.isInWorld, "the caller-owned chassis must survive teardown")
            }
            .thenExecute {
                physics.submitTask("s2 runtime cleanup") {
                    physics.removeBody(chassis)
                }
            }
            .thenWaitUntil {
                helper.assertTrue(!chassis.isInWorld, "the chassis must be removed after the test")
            }
            .thenSucceed()
    }

    /**
     * A closed runtime must not leak on a second close.
     *
     * Lifecycle bugs usually show up as a double teardown, so the second call is
     * checked explicitly rather than assumed.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 800)
    fun closingTwiceIsIdempotent(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -4..4) {
            for (z in -5..5) {
                for (y in 1..8) {
                    helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
                }
            }
        }
        val spec = compile()
        val host = object : PhysicsHost {
            override val physicsLevel: PhysicsLevel get() = physics
            override val allPhysicsBodies: MutableMap<String, PhysicsCollisionObject> = ConcurrentHashMap()
        }
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 5f, anchor.z + 0.5f)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), 0f)
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val first = java.util.concurrent.atomic.AtomicReference<CompletableFuture<Void>?>(null)
        val second = java.util.concurrent.atomic.AtomicReference<CompletableFuture<Void>?>(null)

        val bodyStage = physics.submitTask("s2 idempotent chassis") {
            chassis.isKinematic = true
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.PHYSICS_BODY
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.owner = host
            physics.addBody(chassis)
        }

        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(bodyStage.isDone, "the chassis task did not run")
                bodyStage.join().let { }
                helper.assertTrue(chassis.isInWorld, "the chassis must be in the world")
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil {
                helper.assertTrue(runtime.get()?.isCreated == true, "the runtime must be created")
            }
            .thenExecute {
                val created = runtime.get() ?: error("no runtime")
                first.set(created.closeAsync())
            }
            .thenWaitUntil {
                helper.assertTrue(first.get()?.isDone == true, "the first close must finish")
            }
            .thenExecute {
                second.set(runtime.get()?.closeAsync())
            }
            .thenWaitUntil {
                val future = second.get() ?: error("the second close was not started")
                helper.assertTrue(future.isDone, "a second close must complete immediately")
                helper.assertTrue(
                    !future.isCompletedExceptionally,
                    "a second close must not fail: ${future.getNow(null)}",
                )
                val created = runtime.get() ?: error("no runtime")
                helper.assertTrue(created.isClosed, "the runtime must stay closed")
                helper.assertTrue(created.wheelCount == 0 && created.runCount == 0, "nothing may be re-created")
            }
            .thenExecute { physics.submitTask("s2 idempotent cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * A failure after the wheels exist must roll those wheels and mounts back.
     * Clearing the published lists is not enough; the ledger must be empty.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 800)
    fun aFailedCreateRollsBackTheWheels(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val spec = compile()
        val host = object : PhysicsHost {
            override val physicsLevel: PhysicsLevel get() = physics
            override val allPhysicsBodies: MutableMap<String, PhysicsCollisionObject> = ConcurrentHashMap()
        }
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), 0f)
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s2 fail setup") {
            chassis.isKinematic = true
            chassis.setPhysicsLocation(Vector3f(anchor.x + 0.5f, anchor.y + 5f, anchor.z + 0.5f))
            chassis.collisionGroup = CollisionGroups.PHYSICS_BODY
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.owner = host
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                created.failCreateAfterBodies = true
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil {
                val created = runtime.get() ?: error("no runtime")
                helper.assertTrue(created.isFailed, "the injected failure must fail the create")
                helper.assertFalse(created.isCreated, "a failed create must not become active")
                helper.assertFalse(
                    created.hasResidual,
                    "rollback must release wheels and mounts, joints=${created.residualJointCount} bodies=${created.residualBodyCount}",
                )
            }
            .thenExecute { physics.submitTask("s2 fail cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 800)
    fun aFailedCreateAfterRunsRollsBack(helper: GameTestHelper) =
        failedCreateRollsBack(helper, "runs") { it.failCreateAfterRuns = true }

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 800)
    fun aFailedCreateBeforeEndJointsRollsBack(helper: GameTestHelper) =
        failedCreateRollsBack(helper, "ends") { it.failCreateAfterEnds = true }

    private fun failedCreateRollsBack(
        helper: GameTestHelper,
        label: String,
        inject: (TrackedVehicleRuntime) -> Unit,
    ) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val spec = compile()
        val host = object : PhysicsHost {
            override val physicsLevel: PhysicsLevel get() = physics
            override val allPhysicsBodies: MutableMap<String, PhysicsCollisionObject> = ConcurrentHashMap()
        }
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), 0f)
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val setup = physics.submitTask("s2 fail $label setup") {
            chassis.isKinematic = true
            chassis.setPhysicsLocation(Vector3f(anchor.x + 0.5f, anchor.y + 5f, anchor.z + 0.5f))
            chassis.collisionGroup = CollisionGroups.PHYSICS_BODY
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.owner = host
            physics.addBody(chassis)
        }
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "setup did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                inject(created)
                runtime.set(created)
                created.create()
            }
            .thenWaitUntil {
                val created = runtime.get() ?: error("no runtime")
                helper.assertTrue(created.isFailed, "the injected $label failure must fail the create")
                helper.assertFalse(created.isCreated, "a failed create must not become active")
                helper.assertFalse(
                    created.hasResidual,
                    "rollback must release the $label assembly, joints=${created.residualJointCount} " +
                        "bodies=${created.residualBodyCount} runs=${created.residualRuntimeCount}",
                )
            }
            .thenExecute { physics.submitTask("s2 fail $label cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }
}
