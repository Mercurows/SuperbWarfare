package com.atsuishio.superbwarfare.gametest

import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.SparkPhysics
import cn.solarmoon.spark_core.physics.body.CollisionGroups
import cn.solarmoon.spark_core.physics.body.owner
import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.physics.VehiclePhysics
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleCompiler
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleDefinition
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleRuntime
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleSpec
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import kotlinx.serialization.json.Json
import com.atsuishio.superbwarfare.physics.tracked.TrackWheelRole
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.TicketType
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.level.ChunkEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import java.util.concurrent.ConcurrentHashMap

/**
 * S2 lifecycle: the runtime must survive the paths a real vehicle takes.
 *
 * The assembly gate proves the runtime builds and tears down once. These cases
 * prove the paths that actually run in a world: removal, re-entry after a
 * reload, and a level shutdown. A leak here does not show up as a failed
 * assertion in the assembly test; it shows up later as a ghost body that keeps
 * colliding with a vehicle that no longer exists.
 */
@GameTestHolder(Mod.MODID)
@PrefixGameTestTemplate(false)
object TrackedLifecycleGameTests {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun resource(path: String): String {
        val stream = TrackedLifecycleGameTests::class.java.classLoader.getResourceAsStream(path)
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

    private fun floor(helper: GameTestHelper) {
        val anchor = helper.absolutePos(BlockPos.ZERO)
        for (x in -6..6) {
            for (z in -6..6) {
                helper.getLevel().setBlock(anchor.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3)
                for (y in 1..8) {
                    helper.getLevel().setBlock(anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
                }
            }
        }
    }

    /**
     * Removing the entity must take the whole running gear with it.
     *
     * The chassis is removed by `VehiclePhysics`, but the wheels, mounts, runs
     * and end joints belong to the runtime. If its close path were skipped, the
     * level would keep simulating a belt whose owner is gone.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun removingTheVehicleReleasesEveryNativeObject(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val origin = Vector3f(anchor.x + 0.5f, anchor.y + 4.0f, anchor.z + 0.5f)
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), 0f)
        val runtime = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val bodyCount = java.util.concurrent.atomic.AtomicInteger()

        val setup = physics.submitTask("lifecycle removal setup") {
            chassis.isKinematic = true
            chassis.setPhysicsLocation(origin)
            chassis.collisionGroup = CollisionGroups.PHYSICS_BODY
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.owner = host
            physics.addBody(chassis)
        }

        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "the setup task did not run")
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
                // Count what exists before teardown so the assertion after it is
                // about a known quantity rather than "nothing obviously wrong".
                bodyCount.set(runtime.get()?.wheelCount ?: 0)
                helper.assertTrue(bodyCount.get() > 0, "the runtime must own wheel bodies")
            }
            .thenExecute { runtime.get()?.closeAsync() }
            .thenWaitUntil {
                val created = runtime.get() ?: error("no runtime")
                helper.assertTrue(created.isClosed, "the runtime must close")
                helper.assertTrue(
                    created.wheelCount == 0,
                    "every wheel body must be released, still ${created.wheelCount} of ${bodyCount.get()}",
                )
                helper.assertTrue(created.runCount == 0, "every run runtime must be released")
                helper.assertTrue(created.endJointCount == 0, "every external end joint must be released")
                helper.assertFalse(
                    created.hasResidual,
                    "the ledger must be empty after close, joints=${created.residualJointCount} " +
                        "bodies=${created.residualBodyCount} runs=${created.residualRuntimeCount}",
                )
            }
            .thenExecute { physics.submitTask("lifecycle removal cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * A closed runtime must be rebuildable at a new pose.
     *
     * This is the shape of a chunk reload: the old assembly is gone and a new
     * one is created from the same compiled data. If the runtime cached a stale
     * body list, the second assembly would silently reuse dead native objects.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun aRuntimeCanBeRebuiltAfterClose(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        floor(helper)
        val spec = compile()
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val firstOrigin = Vector3f(anchor.x + 0.5f, anchor.y + 4.0f, anchor.z + 0.5f)
        val secondOrigin = firstOrigin.add(Vector3f(1.0f, 0f, 1.0f))
        val host = host(physics)
        val chassis = PhysicsRigidBody(BoxCollisionShape(0.9f, 0.4f, 1.6f), 0f)
        val first = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val second = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime?>(null)
        val firstWheels = java.util.concurrent.atomic.AtomicInteger()

        val setup = physics.submitTask("lifecycle rebuild setup") {
            chassis.isKinematic = true
            chassis.setPhysicsLocation(firstOrigin)
            chassis.collisionGroup = CollisionGroups.PHYSICS_BODY
            chassis.setCollideWithGroups(CollisionGroups.NONE)
            chassis.owner = host
            physics.addBody(chassis)
        }

        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(setup.isDone, "the setup task did not run")
                setup.join().let { }
            }
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                first.set(created)
                created.create()
            }
            .thenWaitUntil {
                helper.assertTrue(first.get()?.isCreated == true, "the first assembly must build")
                firstWheels.set(first.get()?.wheelCount ?: 0)
                helper.assertTrue(firstWheels.get() > 0, "the first assembly must own wheels")
            }
            .thenExecute { first.get()?.closeAsync() }
            .thenWaitUntil {
                helper.assertTrue(first.get()?.isClosed == true, "the first assembly must close")
            }
            // Move the hull, then build a fresh runtime at the new pose.
            .thenExecute {
                physics.submitTask("lifecycle rebuild move") {
                    chassis.setPhysicsLocation(secondOrigin)
                }
            }
            .thenIdle(5)
            .thenExecute {
                val created = TrackedVehicleRuntime(physics, host, spec, chassis)
                second.set(created)
                created.create()
            }
            .thenWaitUntil {
                val created = second.get() ?: error("no second runtime")
                helper.assertTrue(created.isCreated, "the second assembly must build after a close")
                helper.assertTrue(
                    created.wheelCount == firstWheels.get(),
                    "the rebuilt runtime must own the same wheels: ${created.wheelCount} vs ${firstWheels.get()}",
                )
                helper.assertTrue(created.runCount > 0, "the rebuilt runtime must own runs")
                helper.assertTrue(created.endJointCount > 0, "the rebuilt runtime must own end joints")
            }
            .thenExecute { second.get()?.closeAsync() }
            .thenWaitUntil {
                helper.assertTrue(second.get()?.isClosed == true, "the second assembly must close")
            }
            .thenExecute { physics.submitTask("lifecycle rebuild cleanup") { physics.removeBody(chassis) } }
            .thenWaitUntil { helper.assertTrue(!chassis.isInWorld, "cleanup must remove the chassis") }
            .thenSucceed()
    }

    /**
     * The real entity path: spawn a 99A, let it assemble, then discard it.
     *
     * This is the only case that exercises `VehiclePhysics.begin` →
     * `VehicleBody.create` → `TrackedVehicleRuntime.create` together, and then
     * the `EntityLeaveLevelEvent` teardown chain. The direct runtime tests above
     * cannot prove that wiring.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 1200)
    fun theRealVehicleEntityAssemblesAndReleasesThroughTheEventPath(helper: GameTestHelper) {
        val physics = SparkPhysics.get(helper.getLevel())
        val ownerBodies = java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Int>?>(null)
        floor(helper)
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val vehicle = ModEntities.ZTZ_99A.get().create(helper.getLevel())
        helper.assertTrue(vehicle != null, "ZTZ-99A entity could not be created")
        vehicle!!.setPos(anchor.x + 0.5, anchor.y + 4.0, anchor.z + 0.5)
        helper.getLevel().addFreshEntity(vehicle)

        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(
                    VehiclePhysics.isManaged(vehicle),
                    "the 99A must be claimed by the physics layer",
                )
            }
            .thenWaitUntil {
                helper.assertTrue(
                    VehiclePhysics.hasBody(vehicle),
                    "the 99A must receive a chassis body",
                )
            }
            .thenIdle(40)
            .thenExecute {
                // Discarding the entity must run the leave path, which closes the
                // runtime before the hull is removed.
                vehicle.discard()
            }
            .thenWaitUntil {
                helper.assertFalse(
                    VehiclePhysics.isManaged(vehicle),
                    "a discarded vehicle must be released by the physics layer",
                )
            }
            .thenWaitUntil {
                val close = VehiclePhysics.closeFuture(vehicle)
                helper.assertTrue(close != null, "discard must publish a native close future")
                helper.assertTrue(close!!.isDone, "discard close must finish before residuals are counted")
                helper.assertTrue(!close.isCompletedExceptionally, "discard close failed")
            }
            .thenIdle(20)
            .thenExecute {
                helper.assertFalse(
                    VehiclePhysics.hasBody(vehicle),
                    "a discarded vehicle must not keep a chassis body",
                )
            }
            .thenWaitUntil {
                val pending = ownerBodies.get()
                if (pending == null || pending.isDone) {
                    val count = if (pending != null && pending.isDone) pending.getNow(-1) else -1
                    if (count == 0) return@thenWaitUntil
                    ownerBodies.set(physics.submitTask("lifecycle entity residual") {
                        val host = SparkPhysics.host(vehicle)
                        physics.world.rigidBodyList.count { it.owner === host }
                    })
                }
                helper.assertTrue(false, "waiting for vehicle-owned bodies to leave the world")
            }
            .thenSucceed()
    }

    /**
     * NBT save/load must restore the hull pose the physics layer saved, then a
     * fresh entity must be claimable again.
     *
     * This is the production `VehiclePhysics.save` / `load` pair, not a second
     * runtime constructed by the test. Chunk unload is the same leave/join path
     * as [theRealVehicleEntityAssemblesAndReleasesThroughTheEventPath].
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 1200)
    fun sparkPhysicsNbtRoundTripsAndAReloadedEntityIsClaimedAgain(helper: GameTestHelper) {
        floor(helper)
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val vehicle = ModEntities.ZTZ_99A.get().create(helper.getLevel())
        helper.assertTrue(vehicle != null, "ZTZ-99A entity could not be created")
        vehicle!!.setPos(anchor.x + 0.5, anchor.y + 4.0, anchor.z + 0.5)
        helper.getLevel().addFreshEntity(vehicle)

        val saved = CompoundTag()
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(VehiclePhysics.isManaged(vehicle), "the 99A must be claimed before save")
            }
            .thenIdle(20)
            .thenExecute {
                VehiclePhysics.save(vehicle, saved)
                helper.assertTrue(saved.contains("SparkPhysics"), "save must write SparkPhysics")
                val data = saved.getCompound("SparkPhysics")
                helper.assertTrue(data.getInt("Version") == 1, "SparkPhysics version must be written")
                helper.assertTrue(
                    (0..3).all { java.lang.Float.isFinite(data.getFloat("Q$it")) } &&
                        (0..5).all { java.lang.Double.isFinite(data.getDouble("V$it")) },
                    "saved pose and velocity must be finite",
                )
                vehicle.discard()
            }
            .thenWaitUntil {
                helper.assertFalse(VehiclePhysics.isManaged(vehicle), "discard must release the first entity")
            }
            .thenWaitUntil {
                val close = VehiclePhysics.closeFuture(vehicle)
                helper.assertTrue(close != null && close.isDone, "NBT source close must finish before reload")
                helper.assertTrue(close != null && !close.isCompletedExceptionally, "NBT source close failed")
            }
            .thenExecute {
                val reloaded = ModEntities.ZTZ_99A.get().create(helper.getLevel())
                    ?: error("reload entity could not be created")
                reloaded.setPos(anchor.x + 1.5, anchor.y + 4.0, anchor.z + 1.5)
                VehiclePhysics.load(reloaded, saved)
                helper.getLevel().addFreshEntity(reloaded)
            }
            .thenIdle(5)
            .thenWaitUntil {
                val live = helper.getLevel().getEntitiesOfClass(
                    com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity::class.java,
                    net.minecraft.world.phys.AABB(anchor).inflate(8.0),
                )
                val reloaded = live.firstOrNull { it !== vehicle && !it.isRemoved }
                helper.assertTrue(reloaded != null, "the reloaded entity must still be in the level")
                helper.assertTrue(
                    VehiclePhysics.isManaged(reloaded!!),
                    "a reloaded 99A must be claimed by the physics layer",
                )
            }
            .thenSucceed()
    }

    /**
     * Dropping a chunk's terrain bodies must not delete a vehicle that is still
     * in the level. Entity leave, not the terrain cache, owns mechanism teardown.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun unloadingTerrainDoesNotDestroyAStillLoadedVehicle(helper: GameTestHelper) {
        floor(helper)
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val vehicle = ModEntities.ZTZ_99A.get().create(helper.getLevel())
        helper.assertTrue(vehicle != null, "ZTZ-99A entity could not be created")
        vehicle!!.setPos(anchor.x + 0.5, anchor.y + 4.0, anchor.z + 0.5)
        helper.getLevel().addFreshEntity(vehicle)
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(VehiclePhysics.hasBody(vehicle), "the 99A must assemble before terrain unload") }
            .thenExecute {
                physics.terrainManager.onChunkUnloaded(vehicle.chunkPosition())
            }
            .thenIdle(20)
            .thenExecute {
                helper.assertTrue(VehiclePhysics.isManaged(vehicle), "terrain unload must not drop a loaded vehicle")
                helper.assertTrue(VehiclePhysics.hasBody(vehicle), "terrain unload must not remove the chassis")
                helper.assertTrue(physics.isAcceptingTasks(), "terrain unload must not stop the physics level")
            }
            .thenSucceed()
    }

    /**
     * The same close used by level unload, run while the level is still alive.
     * Residuals must be gone before any native world destroy.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun closingVehiclesBeforeLevelShutdownReleasesOwnedBodies(helper: GameTestHelper) {
        floor(helper)
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val vehicle = ModEntities.ZTZ_99A.get().create(helper.getLevel())
        helper.assertTrue(vehicle != null, "ZTZ-99A entity could not be created")
        vehicle!!.setPos(anchor.x + 0.5, anchor.y + 4.0, anchor.z + 0.5)
        helper.getLevel().addFreshEntity(vehicle)
        val close = java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>?>(null)
        val owned = java.util.concurrent.atomic.AtomicInteger(-1)
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(VehiclePhysics.hasBody(vehicle), "the 99A must assemble") }
            .thenExecute {
                close.set(VehiclePhysics.closeVehicles(helper.getLevel()))
                // The entity is still ticking. Discard it in the same tick so
                // begin() cannot admit a replacement before this close finishes.
                vehicle.discard()
            }
            .thenWaitUntil {
                val future = close.get() ?: error("close was not started")
                helper.assertTrue(future.isDone, "closeVehicles must finish while the level is alive")
                helper.assertTrue(!future.isCompletedExceptionally, "closeVehicles failed: ${future.getNow(null)}")
                helper.assertFalse(VehiclePhysics.isManaged(vehicle), "a closed vehicle must leave the management table")
                helper.assertTrue(physics.isAcceptingTasks(), "this close must not destroy the shared physics level")
            }
            .thenExecute {
                physics.submitTask("count after closeVehicles") {
                    val host = SparkPhysics.host(vehicle)
                    owned.set(physics.world.rigidBodyList.count { it.owner === host })
                }
            }
            .thenWaitUntil {
                helper.assertTrue(owned.get() == 0, "closeVehicles must release owned bodies before level destroy, still ${owned.get()}")
            }
            .thenSucceed()
    }

    /** A non-zero saved velocity must come back when the entity re-enters at the same pose. */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 1200)
    fun savedMotionIsRestoredWhenTheEntityReenters(helper: GameTestHelper) {
        floor(helper)
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val vehicle = ModEntities.ZTZ_99A.get().create(helper.getLevel())
        helper.assertTrue(vehicle != null, "ZTZ-99A entity could not be created")
        vehicle!!.setPos(anchor.x + 0.5, anchor.y + 4.0, anchor.z + 0.5)
        helper.getLevel().addFreshEntity(vehicle)
        val saved = CompoundTag()
        val savedSpeed = java.util.concurrent.atomic.AtomicReference(0.0)
        // The pose that belongs to the saved velocity. Reloading at the
        // post-discard position of a still-moving vehicle would look like a
        // redeploy to `VehiclePhysics.begin`, which deliberately drops motion.
        val savedPose = java.util.concurrent.atomic.AtomicReference(net.minecraft.world.phys.Vec3.ZERO)
        // Rolling resistance starts acting as soon as the hull is back on the
        // ground, so the restored speed is a transient. Record the largest
        // matching value seen instead of demanding it survive an arbitrary wait.
        val restoredSpeed = java.util.concurrent.atomic.AtomicReference(0.0)
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(VehiclePhysics.hasBody(vehicle), "the 99A must be ready before the impulse") }
            .thenExecute { VehiclePhysics.impulse(vehicle, net.minecraft.world.phys.Vec3(0.0, 0.0, 80_000.0)) }
            .thenWaitUntil {
                VehiclePhysics.save(vehicle, saved)
                val speed = saved.getCompound("SparkPhysics").getDouble("V2")
                helper.assertTrue(kotlin.math.abs(speed) > 0.5, "waiting until the impulse is in the saved velocity, was $speed")
                savedSpeed.set(speed)
                savedPose.set(vehicle.position())
            }
            .thenExecute { vehicle.discard() }
            .thenWaitUntil { helper.assertFalse(VehiclePhysics.isManaged(vehicle), "discard must release the first entity") }
            .thenWaitUntil {
                val close = VehiclePhysics.closeFuture(vehicle)
                helper.assertTrue(close != null && close.isDone, "saved-motion source close must finish before reload")
                helper.assertTrue(close != null && !close.isCompletedExceptionally, "saved-motion source close failed")
            }
            .thenExecute {
                val reloaded = ModEntities.ZTZ_99A.get().create(helper.getLevel())
                    ?: error("reload entity could not be created")
                reloaded.setPos(savedPose.get().x, savedPose.get().y, savedPose.get().z)
                VehiclePhysics.load(reloaded, saved)
                helper.getLevel().addFreshEntity(reloaded)
            }
            .thenWaitUntil {
                val live = helper.getLevel().getEntitiesOfClass(
                    com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity::class.java,
                    net.minecraft.world.phys.AABB(anchor).inflate(8.0),
                )
                val reloaded = live.firstOrNull { it !== vehicle && !it.isRemoved }
                helper.assertTrue(reloaded != null && VehiclePhysics.hasBody(reloaded), "the reloaded 99A must have a body")
            }
            .thenWaitUntil {
                val live = helper.getLevel().getEntitiesOfClass(
                    com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity::class.java,
                    net.minecraft.world.phys.AABB(anchor).inflate(8.0),
                )
                val reloaded = live.firstOrNull { it !== vehicle && !it.isRemoved }
                    ?: error("reloaded entity disappeared")
                val again = CompoundTag()
                VehiclePhysics.save(reloaded, again)
                val speed = again.getCompound("SparkPhysics").getDouble("V2")
                if (kotlin.math.sign(speed) == kotlin.math.sign(savedSpeed.get()) &&
                    kotlin.math.abs(speed) > kotlin.math.abs(restoredSpeed.get())
                ) {
                    restoredSpeed.set(speed)
                }
                val diagnostic = VehiclePhysics.diagnostic(reloaded)
                // A reloaded hull holds its motion while the terrain mesh for its
                // new position is still being built, and rolling resistance starts
                // decaying it once the hull is supported. Sample the restored
                // value rather than demanding it survive an arbitrary wait.
                helper.assertTrue(
                    kotlin.math.abs(restoredSpeed.get()) > 0.2,
                    "waiting for re-entry to restore the saved velocity, saved ${savedSpeed.get()} " +
                        "observed ${restoredSpeed.get()} now $speed diagnostic=$diagnostic",
                )
            }
            .thenSucceed()
    }

    /**
     * Unload the chunk that actually contains the vehicle, then load it again.
     *
     * This is not the terrain-cache notification. The vehicle is told not to pin
     * the chunk, the force ticket is removed, and the test waits for Minecraft
     * to unload that chunk. Physics bodies must be gone before the chunk comes
     * back with the same UUID. Velocity can decay while the chunk ticket expires,
     * so this case does not claim that the impulse survives the wait.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 1800)
    fun unloadingTheVehiclesChunkReleasesBodiesThenReloadRestoresMotion(helper: GameTestHelper) {
        val level = helper.getLevel() as ServerLevel
        val physics = SparkPhysics.get(level)
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val far = ChunkPos((anchor.x shr 4) + 96, (anchor.z shr 4) + 96 + (anchor.x and 15))
        val origin = BlockPos(far.minBlockX + 8, 4, far.minBlockZ + 8)
        val unloaded = java.util.concurrent.atomic.AtomicBoolean(false)
        val owned = java.util.concurrent.atomic.AtomicInteger(-1)
        val listener = ChunkUnloadWatcher(level, far, unloaded)
        MinecraftForge.EVENT_BUS.register(listener)
        level.chunkSource.updateChunkForced(far, true)
        val vehicle = ModEntities.ZTZ_99A.get().create(level)
        helper.assertTrue(vehicle != null, "ZTZ-99A entity could not be created")
        vehicle!!.override = "{\"KeepChunkLoaded\":false}"
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(level.hasChunk(far.x, far.z), "the far chunk must load before the vehicle is placed") }
            .thenExecute {
                for (x in -3..3) for (z in -3..3) for (y in 0..8) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
                }
                for (x in -3..3) for (z in -3..3) {
                    level.setBlock(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3)
                }
                vehicle.setPos(origin.x + 0.5, origin.y + 4.0, origin.z + 0.5)
                level.addFreshEntity(vehicle)
            }
            .thenWaitUntil { helper.assertTrue(VehiclePhysics.hasBody(vehicle), "the far 99A must assemble before its chunk unloads") }
            .thenExecute { level.chunkSource.updateChunkForced(far, false) }
            .thenWaitUntil {
                helper.assertTrue(unloaded.get() || !level.hasChunk(far.x, far.z), "waiting for the vehicle chunk to unload, hasChunk=${level.hasChunk(far.x, far.z)}")
                helper.assertFalse(VehiclePhysics.isManaged(vehicle), "chunk unload must drop the vehicle from the management table")
                val close = VehiclePhysics.closeFuture(vehicle)
                helper.assertTrue(close != null && close.isDone, "chunk unload close must finish before bodies are counted")
                helper.assertTrue(close != null && !close.isCompletedExceptionally, "chunk unload close failed")
            }
            .thenExecute {
                physics.submitTask("count after chunk unload") {
                    val host = SparkPhysics.host(vehicle)
                    owned.set(physics.world.rigidBodyList.count { it.owner === host })
                }
            }
            .thenWaitUntil {
                helper.assertTrue(owned.get() == 0, "chunk unload must release owned bodies before the chunk returns, still ${owned.get()}")
            }
            .thenExecute { level.chunkSource.updateChunkForced(far, true) }
            .thenWaitUntil {
                val live = level.getEntitiesOfClass(
                    com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity::class.java,
                    AABB(origin).inflate(8.0),
                )
                val reloaded = live.firstOrNull { it !== vehicle && !it.isRemoved }
                helper.assertTrue(reloaded != null && VehiclePhysics.hasBody(reloaded), "reloading the chunk must restore a 99A body")
            }
            .thenIdle(2)
            .thenExecute {
                val live = level.getEntitiesOfClass(
                    com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity::class.java,
                    AABB(origin).inflate(8.0),
                )
                val reloaded = live.firstOrNull { it !== vehicle && !it.isRemoved }
                    ?: error("reloaded entity disappeared")
                val again = CompoundTag()
                VehiclePhysics.save(reloaded, again)
                val data = again.getCompound("SparkPhysics")
                helper.assertTrue(reloaded.uuid == vehicle.uuid, "chunk reload must restore the same entity, was ${reloaded.uuid}")
                helper.assertTrue(data.getInt("Version") == 1, "chunk reload must write a physics snapshot")
                helper.assertTrue(
                    (0..5).all { java.lang.Double.isFinite(data.getDouble("V$it")) },
                    "reloaded physics velocity must be finite",
                )
                reloaded.discard()
                level.chunkSource.updateChunkForced(far, false)
                MinecraftForge.EVENT_BUS.unregister(listener)
            }
            .thenSucceed()
    }

    /**
     * A vehicle that still asks for chunk pins must keep its chunk loaded.
     * Releasing that pin, then removing the tickets, must unload the chunk and
     * bring the same entity back without owned bodies in between.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 1800)
    fun aPinningVehicleKeepsItsChunkUntilThePinIsReleased(helper: GameTestHelper) {
        val level = helper.getLevel() as ServerLevel
        val physics = SparkPhysics.get(level)
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val far = ChunkPos((anchor.x shr 4) + 160, (anchor.z shr 4) + 160 + (anchor.z and 15))
        val origin = BlockPos(far.minBlockX + 8, 4, far.minBlockZ + 8)
        val unloaded = java.util.concurrent.atomic.AtomicBoolean(false)
        val owned = java.util.concurrent.atomic.AtomicInteger(-1)
        val listener = ChunkUnloadWatcher(level, far, unloaded)
        MinecraftForge.EVENT_BUS.register(listener)
        level.chunkSource.updateChunkForced(far, true)
        val vehicle = ModEntities.ZTZ_99A.get().create(level) ?: error("ZTZ-99A entity could not be created")
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(level.hasChunk(far.x, far.z), "the pinned chunk must load") }
            .thenExecute {
                for (x in -3..3) for (z in -3..3) for (y in 0..8) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
                }
                for (x in -3..3) for (z in -3..3) {
                    level.setBlock(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3)
                }
                vehicle.setPos(origin.x + 0.5, origin.y + 4.0, origin.z + 0.5)
                level.addFreshEntity(vehicle)
            }
            .thenWaitUntil { helper.assertTrue(VehiclePhysics.hasBody(vehicle), "the pinning 99A must assemble") }
            .thenExecute { level.chunkSource.updateChunkForced(far, false) }
            .thenIdle(40)
            .thenExecute {
                helper.assertTrue(level.hasChunk(far.x, far.z), "an active chunk pin must keep the vehicle chunk loaded")
                helper.assertTrue(VehiclePhysics.isManaged(vehicle), "the pin must not be treated as an unload")
                vehicle.override = "{\"KeepChunkLoaded\":false}"
                dropPinTickets(level, vehicle)
            }
            .thenWaitUntil {
                helper.assertTrue(unloaded.get() || !level.hasChunk(far.x, far.z), "releasing the pin must let the chunk unload")
                helper.assertFalse(VehiclePhysics.isManaged(vehicle), "the released pin must drop the vehicle")
                val close = VehiclePhysics.closeFuture(vehicle)
                helper.assertTrue(close != null && close.isDone, "pin release close must finish before bodies are counted")
                helper.assertTrue(close != null && !close.isCompletedExceptionally, "pin release close failed")
            }
            .thenExecute {
                physics.submitTask("count after pin release") {
                    val host = SparkPhysics.host(vehicle)
                    owned.set(physics.world.rigidBodyList.count { it.owner === host })
                }
            }
            .thenWaitUntil {
                helper.assertTrue(owned.get() == 0, "pin release must clear owned bodies, still ${owned.get()}")
            }
            .thenExecute { level.chunkSource.updateChunkForced(far, true) }
            .thenWaitUntil {
                val live = level.getEntitiesOfClass(
                    com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity::class.java,
                    AABB(origin).inflate(8.0),
                )
                val reloaded = live.firstOrNull { it !== vehicle && !it.isRemoved }
                helper.assertTrue(
                    reloaded != null && reloaded.uuid == vehicle.uuid && VehiclePhysics.hasBody(reloaded),
                    "reloading after the pin release must restore the same 99A",
                )
            }
            .thenExecute {
                level.getEntitiesOfClass(
                    com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity::class.java,
                    AABB(origin).inflate(8.0),
                ).forEach { it.discard() }
                level.chunkSource.updateChunkForced(far, false)
                MinecraftForge.EVENT_BUS.unregister(listener)
            }
            .thenSucceed()
    }

    /**
     * Close the End physics world, which no other test uses, and read native
     * bodies from the close listener before teardown.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 900)
    fun closingADedicatedLevelSeesResidualsBeforeNativeDestroy(helper: GameTestHelper) {
        val server = helper.getLevel().server ?: error("no server")
        val end = server.getLevel(Level.END) ?: error("the end level is missing")
        val physics = SparkPhysics.get(end)
        val origin = BlockPos(200, 80, 200)
        val far = ChunkPos(origin)
        val sawWorld = java.util.concurrent.atomic.AtomicBoolean(false)
        val ownedAtClose = java.util.concurrent.atomic.AtomicInteger(-1)
        val vehicle = ModEntities.ZTZ_99A.get().create(end)
        helper.assertTrue(vehicle != null, "ZTZ-99A entity could not be created")
        vehicle!!.override = "{\"KeepChunkLoaded\":false}"
        end.chunkSource.updateChunkForced(far, true)
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(physics.isAcceptingTasks() && end.hasChunk(far.x, far.z), "the end physics world must be accepting") }
            .thenExecute {
                for (x in -3..3) for (z in -3..3) for (y in 0..6) {
                    end.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3)
                }
                for (x in -3..3) for (z in -3..3) {
                    end.setBlock(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3)
                }
                vehicle.setPos(origin.x + 0.5, origin.y + 4.0, origin.z + 0.5)
                end.addFreshEntity(vehicle)
            }
            .thenWaitUntil { helper.assertTrue(VehiclePhysics.hasBody(vehicle), "the end 99A must assemble before its level closes") }
            .thenIdle(10)
            .thenExecute {
                // This listener runs after shutdown participants and before
                // native teardown. Vehicle-owned bodies must already be gone.
                physics.addCloseListener {
                    sawWorld.set(runCatching { physics.world.rigidBodyList }.isSuccess)
                    val host = SparkPhysics.host(vehicle)
                    ownedAtClose.set(runCatching {
                        physics.world.rigidBodyList.count { it.owner === host }
                    }.getOrDefault(-1))
                }
                physics.close()
            }
            .thenExecute {
                helper.assertTrue(sawWorld.get(), "the close listener must run while the native world still exists")
                helper.assertTrue(
                    ownedAtClose.get() == 0,
                    "the shutdown participant must remove vehicle bodies before teardown, still ${ownedAtClose.get()}",
                )
                helper.assertFalse(physics.isAcceptingTasks(), "the dedicated level must be shut down")
                helper.assertFalse(VehiclePhysics.isManaged(vehicle), "level close must drop the vehicle")
                end.chunkSource.updateChunkForced(far, false)
            }
            .thenSucceed()
    }

    /**
     * Automated half of pause point A. It measures bind error, idler tension
     * direction, run closure and belt load. It does not replace the user's eyes.
     */
    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 1200)
    fun aResting99aKeepsItsWheelsOnTheBindPose(helper: GameTestHelper) {
        floor(helper)
        val physics = SparkPhysics.get(helper.getLevel())
        val anchor = helper.absolutePos(BlockPos.ZERO)
        val vehicle = ModEntities.ZTZ_99A.get().create(helper.getLevel())
        helper.assertTrue(vehicle != null, "ZTZ-99A entity could not be created")
        vehicle!!.setPos(anchor.x + 0.5, anchor.y + 4.0, anchor.z + 0.5)
        helper.getLevel().addFreshEntity(vehicle)
        val sample = java.util.concurrent.atomic.AtomicReference<TrackedVehicleRuntime.StaticGearSample?>(null)
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(VehiclePhysics.hasBody(vehicle), "the resting 99A must assemble") }
            .thenIdle(220)
            .thenExecute {
                physics.submitTask("sample resting gear") {
                    sample.set(VehiclePhysics.trackedRuntime(vehicle)?.sampleStaticGear())
                }
            }
            .thenWaitUntil { helper.assertTrue(sample.get() != null, "the resting sample must be taken") }
            .thenExecute {
                val gear = sample.get() ?: error("no sample")
                val wheels = gear.wheels
                helper.assertTrue(gear.runCount >= 2, "both sides must still have runs, count ${gear.runCount}")
                helper.assertTrue(wheels.size >= 8, "the resting gear must still have its wheels, count ${wheels.size}")
                helper.assertTrue(wheels.all { it.bindErrorM.isFinite() && it.bindErrorM < 0.45 }, "a wheel left its bind pose: $wheels")
                val idlers = wheels.filter { it.role == TrackWheelRole.IDLER_GUIDE }
                helper.assertTrue(idlers.isNotEmpty(), "an idler must still exist")
                helper.assertTrue(
                    idlers.all {
                        it.tensionTravelM == null &&
                            it.tensionAxisLength == 0.0 &&
                            it.bindErrorM < 0.05 &&
                            it.orthogonalErrorM < 0.05
                    },
                    "the idler is fixed and must stay on its bind pose, $idlers",
                )
                val left = VehiclePhysics.trackedRuntime(vehicle)?.lastSideContact(com.atsuishio.superbwarfare.physics.tracked.TrackedSide.LEFT)
                val right = VehiclePhysics.trackedRuntime(vehicle)?.lastSideContact(com.atsuishio.superbwarfare.physics.tracked.TrackedSide.RIGHT)
                helper.assertTrue(
                    (left?.normalImpulseNs ?: 0.0) > 0.0 && (right?.normalImpulseNs ?: 0.0) > 0.0,
                    "both belts must carry a resting load, left=$left right=$right",
                )
                helper.assertTrue(vehicle.y > anchor.y + 0.5, "the resting hull fell through the floor, y=${vehicle.y}")
            }
            .thenSucceed()
    }

    /**
     * One listener class for both chunk tests.
     *
     * Two anonymous `object` listeners that both declare `onUnload` compile to
     * separate classes, and the Forge ASM bridge then casts the second to the
     * first listener's type. A single named class avoids that.
     */
    private class ChunkUnloadWatcher(
        private val level: ServerLevel,
        private val chunk: ChunkPos,
        private val unloaded: java.util.concurrent.atomic.AtomicBoolean,
    ) {
        @SubscribeEvent
        fun onUnload(event: ChunkEvent.Unload) {
            if (event.level === level && event.chunk.pos == chunk) unloaded.set(true)
        }
    }

    private fun dropPinTickets(level: ServerLevel, vehicle: com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity) {        val chunks = listOf(
            vehicle.chunkPosition(),
            ChunkPos(BlockPos.containing(vehicle.position().add(16.0, 0.0, 0.0))),
            ChunkPos(BlockPos.containing(vehicle.position().add(0.0, 0.0, 16.0))),
        )
        for (chunk in chunks.distinct()) {
            runCatching { level.chunkSource.removeRegionTicket(TicketType.POST_TELEPORT, chunk, 3, vehicle.id) }
        }
    }
}
