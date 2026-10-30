package com.atsuishio.superbwarfare.gametest

import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.SparkPhysics
import cn.solarmoon.spark_core.physics.body.CollisionGroups
import cn.solarmoon.spark_core.physics.body.owner
import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import cn.solarmoon.spark_core.physics.track.TrackRunRuntime
import cn.solarmoon.spark_core.physics.track.TrackRunSpec
import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.physics.tracked.CompiledBodyKind
import com.atsuishio.superbwarfare.physics.tracked.CompiledTrackSide
import com.atsuishio.superbwarfare.physics.tracked.TrackedJointFactory
import com.atsuishio.superbwarfare.physics.tracked.TrackedRunSpecs
import com.atsuishio.superbwarfare.physics.tracked.TrackedSide
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleCompiler
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleDefinition
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleSpec
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CylinderCollisionShape
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import kotlinx.serialization.json.Json
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * T2 gate: the run topology assembled through the **production** path.
 *
 * The native B2 fixtures use a bare `PhysicsSpace`, which is right for
 * constraint semantics but cannot prove the Core runtime lifecycle. This test
 * runs inside a real server `PhysicsLevel`, creates every run through
 * `TrackRunRuntime`, places it at the same world origin as the chassis, mounts
 * every wheel through `TrackedJointFactory`, and creates the external run ends
 * inside `withLinkBodies(...)` — the documented composition point for a vehicle
 * runtime.
 *
 * Placement matters: compiled run and wheel positions are **chassis-local**.
 * The runtime creates a link at the spec position, which is also chassis-local,
 * so `reset(...)` is used to move each run to `origin + compiledLocal`. Without
 * that step the runs would sit at the world origin while the wheels sit at
 * `origin`, and the anchors would never line up.
 *
 * It uses the complete single side (eight members, eight runs) with eight wheel
 * bodies and eight mount constraints under a fixed chassis. It carries no load:
 * that is the native B2.3/B2.4 fixtures' job.
 *
 * Nothing blocks the server thread on a physics task. Each stage is queued and
 * the sequence waits for its observable result, because a physics task only runs
 * during a level tick.
 */
@GameTestHolder(Mod.MODID)
@PrefixGameTestTemplate(false)
object TrackedRunRuntimeGameTests {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Read a mod resource from the classpath.
     *
     * The game test server runs with a different working directory than Gradle,
     * so a `src/main/resources` path is not available. The mod's own resources
     * are on the classpath in both development and production.
     */
    private fun resource(path: String): String {
        val stream = TrackedRunRuntimeGameTests::class.java.classLoader.getResourceAsStream(path)
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
    @GameTest(template = "empty", timeoutTicks = 600)
    fun aCompleteSideAssemblesThroughTheProductionRuntime(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -3..3) {
            for (z in -3..3) {
                for (y in 1..6) {
                    helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
                }
            }
        }

        val spec = compile()
        val side: CompiledTrackSide = spec.side(TrackedSide.LEFT)
        val specs: List<TrackRunSpec> = TrackedRunSpecs.compileRuns(spec, TrackedSide.LEFT)
        helper.assertTrue(
            specs.size == side.runs.size,
            "expected one spec per run, got ${specs.size} for ${side.runs.size}",
        )

        val host = object : PhysicsHost {
            override val physicsLevel: PhysicsLevel get() = physics
            override val allPhysicsBodies: MutableMap<String, PhysicsCollisionObject> = ConcurrentHashMap()
        }

        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 5f, anchor.z + 0.5f)
        // A static chassis: this test proves assembly, not load carrying, so the
        // hull must not move and invalidate the anchors it is measuring.
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), 0f)
        val wheelBodies = ArrayList<PhysicsRigidBody>()
        val mountJoints = ArrayList<New6Dof>()
        val runtimes = ArrayList<TrackRunRuntime>()
        val runBodies = ArrayList<PhysicsRigidBody>()
        val externalJoints = ArrayList<New6Dof>()
        val closeFutures = ArrayList<CompletableFuture<Void>>()
        val jointCount = AtomicInteger()
        // Largest first-end anchor gap measured at joint-creation time. Measuring
        // later would include suspension sag, which is motion, not placement.
        val assemblyGap = java.util.concurrent.atomic.AtomicReference(0.0f)

        // Stage 1: chassis and wheel bodies on the physics thread.
        val bodyStage = physics.submitTask("b2 runtime bodies") {
            chassis.isKinematic = true
            chassis.setPhysicsLocation(origin)
            // Assembly-only fixture: the chassis must not push the runs. A run's
            // default mask includes CHASSIS, so leaving the hull in that group
            // makes every run created inside the hull box get ejected and the
            // anchors no longer describe placement. Bullet requires a group with
            // exactly one bit, so PHYSICS_BODY (not NONE) is the inert choice.
            chassis.collisionGroup = CollisionGroups.PHYSICS_BODY
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.owner = host
            physics.addBody(chassis)
            for (wheel in side.wheels) {
                val shape = CylinderCollisionShape(
                    Vector3f(wheel.halfWidth.toFloat(), wheel.radius.toFloat(), wheel.radius.toFloat()),
                    PhysicsSpace.AXIS_X,
                ).apply { margin = .001f }
                // Assembly-only fixture: the wheels are kinematic so they stay at
                // the compiled bind pose while the runs are created in later
                // stages. A dynamic wheel would sag inside its prismatic mount
                // (or free-fall before the belt exists) and the anchors would no
                // longer describe placement. Gravity response is B2.2/B2.4.
                val body = PhysicsRigidBody(shape, 0f)
                body.isKinematic = true
                body.setPhysicsLocation(
                    origin.add(
                        Vector3f(
                            wheel.assemblyCenter.x.toFloat(),
                            wheel.assemblyCenter.y.toFloat(),
                            wheel.assemblyCenter.z.toFloat(),
                        ),
                    ),
                )
                body.owner = host
                // Likewise: no wheel-run contact in this assembly test. The
                // production belt does rest on its wheels, but that interaction
                // is a load question (B2.2/B2.3), not a placement question.
                body.collisionGroup = CollisionGroups.PHYSICS_BODY
                body.setCollideWithGroups(CollisionGroups.NONE)
                body.isContactResponse = false
                physics.addBody(body)
                wheelBodies += body
            }
            // The real mount constraints: the same plans the vehicle runtime will
            // use, not a free-floating wheel row.
            for (mount in side.mounts) {
                val plan = mount.joint ?: continue
                val joint = TrackedJointFactory.create(plan, chassis, wheelBodies[mount.wheelIndex])
                physics.addJoint(joint)
                mountJoints += joint
            }
        }

        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(
                    bodyStage.isDone,
                    "the body-creation task did not run",
                )
                bodyStage.join().let { } // rethrows the physics-thread failure, if any
                helper.assertTrue(wheelBodies.size == side.wheels.size, "wheel bodies were not created")
                helper.assertTrue(
                    mountJoints.size == side.mounts.count { it.joint != null },
                    "every wheel must be mounted, got ${mountJoints.size}",
                )
            }
            // Stage 2: create every run through the production runtime.
            //
            // `TrackedRunSpecs.compileRuns` emits **chassis-local** link positions,
            // and `TrackRunRuntime.create()` places a link at exactly that value.
            // A vehicle runtime therefore has to translate the spec to the chassis
            // world pose before creation. This test does the same translation here
            // rather than calling `reset()` afterwards: reset runs on the physics
            // thread, so the link would free-fall for the ticks in between and the
            // external ends would be built away from the wheels.
            .thenExecute {
                for ((index, _) in side.runs.withIndex()) {
                    val local = specs[index]
                    val link = local.links.single()
                    val placed = TrackRunSpec(
                        name = local.name,
                        links = listOf(
                            link.copy(
                                position = Vec3(
                                    origin.x + link.position.x,
                                    origin.y + link.position.y,
                                    origin.z + link.position.z,
                                ),
                            ),
                        ),
                        joints = local.joints,
                        collisionGroup = local.collisionGroup,
                        collideWithGroups = local.collideWithGroups,
                        collideWithOwnerGroups = local.collideWithOwnerGroups,
                        linearDamping = local.linearDamping,
                        angularDamping = local.angularDamping,
                        friction = local.friction,
                        restitution = local.restitution,
                        continuousCollision = local.continuousCollision,
                        maxLinkForceN = local.maxLinkForceN,
                        maxLinkTorqueImpulseNms = local.maxLinkTorqueImpulseNms,
                        // The runs are created in one stage and joined in the next;
                        // with world gravity they would fall between those stages.
                        // Assembly-only fixture, so gravity is suspended explicitly
                        // (a documented spec field, not a hidden default).
                        explicitGravity = Vec3.ZERO,
                    )
                    val runtime = TrackRunRuntime(physics, placed, host)
                    runtimes += runtime
                    runtime.create()
                }
            }
            .thenWaitUntil {
                helper.assertTrue(
                    runtimes.size == side.runs.size && runtimes.all { it.isCreated },
                    "every run must be created through TrackRunRuntime",
                )
                for (runtime in runtimes) {
                    helper.assertTrue(runtime.snapshot().linkCount == 1, "a run is one link")
                    helper.assertTrue(runtime.snapshot().finite(), "track run snapshot must be finite")
                }
            }
            // Stage 3: external ends inside withLinkBodies, on the physics thread.
            .thenExecute {
                for ((index, run) in side.runs.withIndex()) {
                    val attachments = run.attachments
                    runtimes[index].withLinkBodies { bodies ->
                        val runBody = bodies.single()
                        synchronized(runBodies) { runBodies += runBody }
                        for (attachment in attachments) {
                            val plan = attachment.joint
                            val bodyA = resolve(plan.bodyA.kind, plan.bodyA.ordinal, wheelBodies, runBody)
                            val bodyB = resolve(plan.bodyB.kind, plan.bodyB.ordinal, wheelBodies, runBody)
                            val joint = TrackedJointFactory.create(plan, bodyA, bodyB)
                            physics.addJoint(joint)
                            synchronized(externalJoints) { externalJoints += joint }
                            jointCount.incrementAndGet()
                            if (attachment == attachments.first()) {
                                // First end is (wheel, run); compare the two world
                                // anchors directly from the bodies we just used.
                                val wheelIndex = if (plan.bodyA.kind == CompiledBodyKind.WHEEL) {
                                    plan.bodyA.ordinal
                                } else {
                                    plan.bodyB.ordinal
                                }
                                val runAnchor = if (plan.bodyA.kind == CompiledBodyKind.RUN) plan.frameInA else plan.frameInB
                                val wheelAnchor = if (plan.bodyA.kind == CompiledBodyKind.WHEEL) plan.frameInA else plan.frameInB
                                val a = worldPoint(runBody, runAnchor.origin)
                                val b = worldPoint(wheelBodies[wheelIndex], wheelAnchor.origin)
                                assemblyGap.set(maxOf(assemblyGap.get(), a.distance(b)))
                            }
                        }
                    }
                }
            }
            .thenWaitUntil {
                helper.assertTrue(
                    jointCount.get() == side.runs.size * 2,
                    "expected ${side.runs.size * 2} external joints, got ${jointCount.get()}",
                )
                // The real proof of placement: every first end had its two anchors
                // coincident at creation, which is only possible if the run, the
                // wheel and the mount all landed in the same world frame.
                helper.assertTrue(
                    assemblyGap.get() < 0.02f,
                    "worst first-end anchor gap at assembly was ${assemblyGap.get()} m; " +
                        "the run was not placed in the chassis frame",
                )
            }
            // Stage 4: teardown. External joints first, then the runtimes, then
            // the wheel mounts and the bodies this test created itself.
            .thenExecute {
                physics.submitTask("b2 runtime teardown") {
                    synchronized(externalJoints) {
                        externalJoints.forEach { physics.removeJoint(it) }
                    }
                    runtimes.forEach { closeFutures += it.closeAsync() }
                }
            }
            .thenWaitUntil {
                helper.assertTrue(runtimes.all { it.isClosed }, "every run must close")
                helper.assertTrue(
                    closeFutures.all { it.isDone && !it.isCompletedExceptionally },
                    "every closeAsync() must complete successfully",
                )
            }
            .thenExecute {
                physics.submitTask("b2 runtime body teardown") {
                    mountJoints.forEach { physics.removeJoint(it) }
                    wheelBodies.forEach { physics.removeBody(it) }
                    physics.removeBody(chassis)
                }
            }
            .thenWaitUntil {
                helper.assertTrue(!chassis.isInWorld, "the chassis must be removed from the world")
                helper.assertTrue(
                    wheelBodies.none { it.isInWorld },
                    "every wheel body must be removed from the world",
                )
                helper.assertTrue(
                    mountJoints.none { physics.world.contains(it) },
                    "every mount joint must be removed from the physics world",
                )
                // The runtime owns the run links; closing must have taken them out
                // of the world rather than only flipping a flag, and the external
                // ends this test created must be gone too.
                synchronized(runBodies) {
                    helper.assertTrue(
                        runBodies.none { it.isInWorld },
                        "every run body must leave the world when its runtime closes",
                    )
                }
                synchronized(externalJoints) {
                    helper.assertTrue(
                        externalJoints.none { physics.world.contains(it) },
                        "every external run-end joint must be removed from the physics world",
                    )
                }
                helper.assertTrue(runBodies.size == side.runs.size, "run bodies must have been captured")
                helper.assertTrue(
                    externalJoints.size == side.runs.size * 2,
                    "expected ${side.runs.size * 2} external joints to tear down, got ${externalJoints.size}",
                )
            }
            .thenSucceed()
    }

    private fun worldPoint(body: PhysicsRigidBody, local: Vec3): Vector3f {
        val offset = body.getPhysicsRotation(null).toRotationMatrix()
            .mult(Vector3f(local.x.toFloat(), local.y.toFloat(), local.z.toFloat()), Vector3f())
        return body.getPhysicsLocation(null).add(offset)
    }

    private fun resolve(
        kind: CompiledBodyKind,
        ordinal: Int,
        wheelBodies: List<PhysicsRigidBody>,
        runBody: PhysicsRigidBody,
    ): PhysicsRigidBody = when (kind) {
        CompiledBodyKind.WHEEL -> wheelBodies[ordinal]
        CompiledBodyKind.RUN -> runBody
        CompiledBodyKind.CHASSIS -> error("a run end cannot reference the chassis")
    }
}
