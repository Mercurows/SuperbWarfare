package com.atsuishio.superbwarfare.physics.tracked

import com.atsuishio.superbwarfare.physics.CollisionPart
import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.PhysicsSuspensionRow
import cn.solarmoon.spark_core.physics.SuspensionRowSpec
import cn.solarmoon.spark_core.physics.SuspensionRowState
import cn.solarmoon.spark_core.physics.body.CollisionGroups
import cn.solarmoon.spark_core.physics.body.collideWithOwnerGroups
import cn.solarmoon.spark_core.physics.body.isColliding
import cn.solarmoon.spark_core.physics.body.name
import cn.solarmoon.spark_core.physics.body.onPrePhysicsTick
import cn.solarmoon.spark_core.physics.body.onResolvedContact
import cn.solarmoon.spark_core.physics.body.owner
import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import cn.solarmoon.spark_core.physics.track.TrackRunAssembly
import cn.solarmoon.spark_core.physics.track.TrackRunLinkSpec
import cn.solarmoon.spark_core.physics.track.TrackRunRuntime
import cn.solarmoon.spark_core.physics.track.TrackRunSpec
import cn.solarmoon.spark_core.event.PhysicsContactConceivedEvent
import cn.solarmoon.spark_core.event.PhysicsContactEvent
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.ContactPointFlag
import com.jme3.bullet.collision.ManifoldPoints
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.collision.shapes.CylinderCollisionShape
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.objects.PhysicsRigidBody
import cn.solarmoon.spark_core.util.Subscription
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import kotlin.math.abs
import kotlin.math.sqrt
import com.jme3.math.Matrix3f
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import cn.solarmoon.spark_core.physics.terrain.TerrainRegionSnapshotResult
import cn.solarmoon.spark_core.physics.terrain.TerrainRevisionToken
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.max

/**
 * S2 production runtime: one tracked vehicle's complete running gear.
 *
 * This is the first place the audited compiled mechanism is assembled **outside**
 * a test fixture. It composes exactly the same three production pieces the B2
 * fixtures use:
 *
 * ```
 * TrackedRunSpecs.compileRuns  ->  TrackRunRuntime   (Core link lifecycle)
 * TrackedJointFactory.create   ->  New6Dof           (mounts and run ends)
 * PhysicsSuspensionRow.force   ->  paired forces     (springs and dampers)
 * ```
 *
 * ## What this class owns
 *
 * - one dynamic wheel body per compiled wheel, per side;
 * - one mount constraint per compiled mount (spring, tensioner or fixed);
 * - one `TrackRunRuntime` per compiled run, created through the Core runtime so
 *   the production link lifecycle is the one being exercised;
 * - the external run-end constraints, created inside `withLinkBodies` — the
 *   documented composition point;
 * - the suspension rows, applied as internal action/reaction pairs each substep.
 *
 * ## What it deliberately does not do
 *
 * - It does not own the chassis. The caller creates and owns the hull body and
 *   passes it in; the runtime only constrains to it.
 * - Drive is a belt-terrain contact target. It is not a force on the chassis
 *   or a fixed wheel; authored torque only caps the contact friction.
 * - It does not decide terrain policy. Terrain is Minecraft AABB collision, S3.
 *
 * ## Threading
 *
 * Construction stores data only. [create] and [closeAsync] queue work on the
 * owning [PhysicsLevel]'s physics thread and never block the caller.
 * [applySuspension] must be called on the physics thread (from the chassis's
 * pre-step hook).
 */
class TrackedVehicleRuntime(
    private val level: PhysicsLevel,
    private val host: PhysicsHost,
    val spec: TrackedVehicleSpec,
    private val chassis: PhysicsRigidBody,
    /**
     * Gravity applied to every running-gear body, m/s².
     *
     * The chassis is integrated by the caller with its own explicit force, so
     * the running gear must not silently inherit a different world value. Wheel
     * ends and runs are both protected with this vector; exactly one path owns
     * a body's gravity.
     */
    private val gravity: Vector3f = Vector3f(0f, -9.81f, 0f),
) : AutoCloseable {

    private enum class State { NEW, CREATING, ACTIVE, CLOSING, CLOSED, FAILED }

    private val state = AtomicReference(State.NEW)
    private val createFuture = AtomicReference<CompletableFuture<Void>?>()
    /** The only terrain collision this runtime is allowed to use. */
    private val terrainProxy = TrackedTerrainMeshProxyRuntime(level, host, spec.vehicleId)
    private val pendingTerrainPlan = AtomicReference<TerrainMeshPlan?>()
    private val terrainApplyQueued = AtomicBoolean(false)
    /** Covers both worker build and physics-thread installation, not just sampling. */
    private val terrainBuildQueued = AtomicBoolean(false)
    private val terrainRebuildRequested = AtomicBoolean(false)
    private val terrainInvalidationQueued = AtomicBoolean(false)
    private val terrainBodiesHeld = AtomicBoolean(false)
    private val terrainRegionRequestId = NEXT_TERRAIN_REGION_ID.incrementAndGet()
    @Volatile private var terrainReady = false
    @Volatile private var terrainMissingReason: String? = null
    @Volatile private var terrainSampledAtTick = Long.MIN_VALUE
    /** Earliest tick at which a failed sample may be retried. */
    @Volatile private var terrainRetryAtTick = Long.MIN_VALUE
    /** Number of installed mesh plans; a rebuild is expected as the window moves. */
    @Volatile var terrainInstallCount: Long = 0
        private set
    /** Number of times the assembly was held because terrain was unavailable. */
    @Volatile var terrainHoldCount: Long = 0
        private set
    /** Desired cache coverage is never used as proof of installed mesh coverage. */
    @Volatile private var requestedTerrainWindow: AABB? = null
    @Volatile private var installedTerrainWindow: AABB? = null
    /** Heading bucket of the installed ramp gate. Distinct from the window. */
    @Volatile private var installedRampBucket: Int = Int.MIN_VALUE
    @Volatile private var installedTerrainRevision: TerrainRevisionToken? = null
    @Volatile private var terrainBuildNanos: Long = 0
    @Volatile private var terrainBuildCount: Long = 0
    @Volatile private var terrainStalePlanCount: Long = 0
    /** Terrain-cache revision the installed mesh was built from. */
    @Volatile var terrainAppliedRevision: Long = Long.MIN_VALUE
        private set
    /** Request id of the installed plan; older plans are discarded on arrival. */
    @Volatile private var terrainAppliedRequestId: Long = -1L
    /** Monotonic request counter; ordering across builds. */
    private val terrainRequestId = java.util.concurrent.atomic.AtomicLong(0)
    /** Bodies whose motion is factor-locked while the mesh is missing. */
    private val terrainWait = TrackedTerrainWaitController()
    /** Every assembly body the wait controller is allowed to touch. */
    private val terrainBodies = ConcurrentHashMap.newKeySet<PhysicsRigidBody>()
    /** Conservative local sampling envelope, derived once from compiled geometry. */
    private val terrainEnvelope: TerrainEnvelope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        TerrainEnvelope.of(spec)
    }
    private val closeFuture = AtomicReference<CompletableFuture<Void>?>()
    private val rollbackFuture = AtomicReference<CompletableFuture<Void>?>()
    /**
     * Bumped by every close. A create stage captures it before doing native work
     * and refuses to commit if a close started meanwhile, so a late stage can
     * never resurrect a runtime that is being torn down.
     */
    private val generation = java.util.concurrent.atomic.AtomicLong(0)
    /** Generation captured when [create] entered [State.CREATING]. */
    private val createGeneration = java.util.concurrent.atomic.AtomicLong(0)

    /** Every native object created by [create], recorded before configuration. */
    private val ledger = TrackedVehicleAssemblyLedger(level)

    /** Wheel bodies per side, in compiled wheel order. */
    @Volatile private var wheelBodies: Map<TrackedSide, List<PhysicsRigidBody>> = emptyMap()
    /** Mount constraints, one per compiled mount that declares a joint. */
    @Volatile private var mountJoints: List<New6Dof> = emptyList()
    /** One runtime per run, in compiled run order, per side. */
    @Volatile private var runtimes: Map<TrackedSide, List<TrackRunRuntime>> = emptyMap()
    /** External run-end constraints, two per run. */
    @Volatile private var endJoints: List<New6Dof> = emptyList()
    private val endJointLock = Any()
    /** Suspension rows per side, keyed by wheel index. */
    @Volatile private var rows: Map<TrackedSide, Map<Int, SuspensionRowSpec>> = emptyMap()

    /** Telemetry of the last applied suspension step; not a pass criterion. */
    private val telemetry = ConcurrentLinkedQueue<SuspensionSample>()

    @Volatile private var lastError: String? = null

    val isCreated: Boolean get() = state.get() == State.ACTIVE
    /** True only when teardown finished; a failed create is not a clean close. */
    val isClosed: Boolean get() = state.get() == State.CLOSED
    /** True when assembly or teardown failed and may still own native objects. */
    val isFailed: Boolean get() = state.get() == State.FAILED
    fun lastError(): String? = lastError

    /** True only when a complete terrain mesh (possibly known-empty) is installed. */
    fun terrainReady(): Boolean = terrainReady && state.get() == State.ACTIVE
    fun terrainRampCount(): Int = terrainProxy.rampCount
    fun terrainTriangleCount(): Int = terrainProxy.triangleCount
    fun terrainWallQuadCount(): Int = terrainProxy.wallQuadCount
    fun terrainInstalledHash(): Long = terrainProxy.installedHash
    /** Immutable copy of the mesh installed in Bullet, or null while waiting. */
    fun terrainDebugMesh(): TrackedTerrainDebugMesh? = terrainProxy.debugMesh
    /** Changes when that mesh is installed or removed. */
    fun terrainDebugEpoch(): Long = terrainProxy.debugEpoch
    fun terrainMissingReason(): String? = terrainMissingReason

    /** Compact native/contact state for GameTest diagnostics; never a pass proof. */
    fun terrainDiagnostics(): String {
        val runs = runSides.entries.joinToString(";") { (body, side) ->
            "$side group=${body.collisionGroup} mask=${body.collideWithGroups} " +
                "ownerMask=${body.collideWithOwnerGroups} kinematic=${body.isKinematic} colliding=${body.isColliding}"
        }
        val window = requestedTerrainWindow
        return "ready=$terrainReady hash=${terrainProxy.installedHash} ramps=${terrainProxy.rampCount} " +
            "rampBucket=$installedRampBucket " +
            "triangles=${terrainProxy.triangleCount} installs=$terrainInstallCount holds=$terrainHoldCount " +
            "held=${terrainWait.size} revision=$terrainAppliedRevision " +
            "request=$terrainAppliedRequestId pending=${terrainRequestId.get()} " +
            "window=[${window?.minX},${window?.minY},${window?.minZ}..${window?.maxX},${window?.maxY},${window?.maxZ}] " +
            "installedWindow=$installedTerrainWindow building=${terrainBuildQueued.get()} " +
            "builds=$terrainBuildCount buildNanos=$terrainBuildNanos stale=$terrainStalePlanCount " +
            "physicsTick=${level.tickCount} droppedFrames=${level.droppedFrameCount} " +
                "stepNanos=${level.latestFrame.stepNanos} stepMetrics=${level.latestFrame.metrics} " +
                "terrainCache={${level.terrainManager.getStats()}} " +
            "contactMotions=$contactMotions conceivedMotions=$conceivedMotions " +
            "support=$supportContacts " +
            "left=${sideWindows[TrackedSide.LEFT]} right=${sideWindows[TrackedSide.RIGHT]} " +
            "missing=$terrainMissingReason runs=[$runs]"
    }

    /**
     * Sample and queue a fresh local mesh. **Game thread only.**
     *
     * No Minecraft world read happens here any more. The sampling window is
     * turned into a detached region request that the terrain cache answers from
     * its own cached section snapshots, so a pass costs a few map lookups instead
     * of a thousand `getBlockState`/`toAabbs` calls. That is what removes the
     * per-tick stall which used to drop physics frames and silently weaken
     * traction.
     *
     * A rebuild still resets every Bullet contact manifold of the vehicle, so the
     * window is snapped to a coarse grid: an idling or slowly rolling hull keeps
     * exactly one mesh. [force] is used by tests after editing terrain.
     */
    /**
     * Keep this vehicle's sampling region alive in the terrain cache. **Game
     * thread only.**
     *
     * The tracked assembly is masked away from block terrain, so it never appears
     * in the activation query that decides which sections the cache builds. Without
     * this declaration the cache would hold no geometry for the vehicle and every
     * region request would answer "missing" forever. It is cheap: one box, one map
     * write, no world read. Call it every tick, independent of the rebuild budget.
     */
    fun maintainTerrainRequest() {
        if (state.get() != State.ACTIVE) return
        val pose = publishedTerrainPose ?: return
        val window = samplingWindow(pose.center) ?: return
        requestedTerrainWindow = window
        level.terrainManager.requestRegion(terrainRegionRequestId, window)
        if (terrainReady && (installedTerrainWindow?.let { !TrackedTerrainSampler.sameWindow(it, window) } != false ||
                installedTerrainRevision != level.terrainManager.revisionTokenAt(window))) {
            markTerrainInvalid()
        }
    }

    private fun samplingWindow(center: Vec3): AABB? = try {
        TrackedTerrainSampler.window(center, terrainEnvelope, spec.terrain)
    } catch (error: IllegalArgumentException) {
        terrainMissingReason = error.message
        markTerrainInvalid()
        null
    }

    fun sampleAndApplyTerrain(world: Level, center: Vec3, force: Boolean = false) {
        if (state.get() != State.ACTIVE || !belongsTo(world)) return
        val now = level.tickCount
        val window = samplingWindow(center) ?: return
        level.terrainManager.requestRegion(terrainRegionRequestId, window)
        requestedTerrainWindow = window
        val previous = installedTerrainWindow
        val sameWindow = previous != null && TrackedTerrainSampler.sameWindow(previous, window)
        val gate = rampGateFor(window)
        // A ready vehicle inside the same snapped window needs nothing. A waiting
        // one must retry, but under a short backoff so a genuinely unloaded area
        // cannot spin the tick away. Turning far enough to change which face is
        // "front" must rebuild even when the window has not moved; that rebuild
        // does not hold the vehicle, the previous mesh stays until the swap.
        if (force) terrainRebuildRequested.set(true)
        if (terrainBuildQueued.get()) return
        if (!force && !terrainRebuildRequested.get() && sameWindow && terrainReady &&
            installedRampBucket == gate.headingBucket &&
            installedTerrainRevision == level.terrainManager.revisionTokenAt(window)) return
        if (!force && now < terrainRetryAtTick) return
        if (force) terrainRetryAtTick = Long.MIN_VALUE
        terrainSampledAtTick = now
        when (val result = level.terrainManager.snapshotRegion(window)) {
            is TerrainRegionSnapshotResult.Missing -> {
                // An incomplete region is never turned into a partial mesh: the
                // vehicle waits instead of driving on geometry we could not see.
                terrainMissingReason = result.reason
                terrainRetryAtTick = now + RETRY_BACKOFF_TICKS
                markTerrainInvalid()
            }
            is TerrainRegionSnapshotResult.Ready -> {
                if (!terrainBuildQueued.compareAndSet(false, true)) return
                terrainRebuildRequested.set(false)
                val snapshot = result.snapshot
                val requestId = terrainRequestId.incrementAndGet()
                val buildGeneration = generation.get()
                val started = System.nanoTime()
                terrainBuildCount += 1
                terrainMissingReason = null
                TrackedTerrainAutoSampler.buildAsync(
                    level = level,
                    snapshot = snapshot,
                    terrain = spec.terrain,
                    requestId = requestId,
                    runtimeGeneration = buildGeneration,
                    rampGate = gate,
                    gear = publishedGearProbes,
                ) { built ->
                    terrainBuildNanos = System.nanoTime() - started
                    if (built is TrackedTerrainSampleResult.Ready) {
                        // Re-checked again on the physics thread; this is only the
                        // cheap early-out for a runtime that closed mid-build.
                        if (generation.get() == buildGeneration && state.get() == State.ACTIVE) {
                            submitTerrainPlan(built.plan)
                        } else {
                            terrainBuildQueued.set(false)
                        }
                    } else {
                        terrainMissingReason = (built as TrackedTerrainSampleResult.Missing).reason
                        terrainRetryAtTick = level.tickCount + RETRY_BACKOFF_TICKS
                        terrainBuildQueued.set(false)
                    }
                }
            }
        }
    }

    /**
     * Install an already-built plan. Safe from any thread; native work is queued
     * on the physics thread and coalesced.
     *
     * Two guards keep a slow build from winning a race it already lost:
     * a plan from an older runtime generation is dropped, and a plan older than
     * the one already installed is dropped. Without them a background builder
     * that finishes late can silently reinstall terrain the vehicle has already
     * driven off.
     */
    fun submitTerrainPlan(plan: TerrainMeshPlan) {
        if (state.get() != State.ACTIVE || plan.runtimeGeneration != generation.get() ||
            plan.requestId <= terrainAppliedRequestId) {
            terrainBuildQueued.set(false)
            return
        }
        pendingTerrainPlan.getAndUpdate { current ->
            if (current == null || current.isSupersededBy(plan)) plan else current
        }
        queueTerrainApplyIfNeeded()
    }

    private fun queueTerrainApplyIfNeeded() {
        if (!terrainApplyQueued.compareAndSet(false, true)) return
        try {
            level.submitTask("tracked terrain mesh ${spec.vehicleId}") {
                try {
                    while (true) {
                        val next = pendingTerrainPlan.getAndSet(null) ?: break
                        if (state.get() != State.ACTIVE) break
                        if (next.runtimeGeneration != generation.get()) continue
                        if (next.requestId <= terrainAppliedRequestId) continue
                        val window = next.region.let { AABB(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ) }
                        val revision = next.terrainRevision
                        if (requestedTerrainWindow?.let { TrackedTerrainSampler.sameWindow(window, it) } != true || revision == null) {
                            terrainStalePlanCount += 1
                            terrainMissingReason = "stale terrain plan:${next.requestId}"
                            markTerrainInvalid()
                            continue
                        }
                        val installed = level.terrainManager.withRevisionToken(window, revision) {
                            val result = terrainProxy.install(
                                next, next.requestId, next.runtimeGeneration, revision,
                                beforeInstall = {
                                    requestedTerrainWindow?.let { TrackedTerrainSampler.sameWindow(window, it) } == true &&
                                        state.get() == State.ACTIVE &&
                                        generation.get() == next.runtimeGeneration &&
                                        terrainRequestId.get() == next.requestId
                                },
                            )
                            if (result != TerrainMeshInstallResult.REJECTED) {
                                terrainAppliedRequestId = next.requestId
                                terrainAppliedRevision = next.terrainRevisionHash
                                installedTerrainRevision = next.terrainRevision
                                installedTerrainWindow = window
                                installedRampBucket = next.rampHeadingBucket
                                terrainReady = true
                                terrainMissingReason = null
                                releaseTerrainHold(restoreVelocity = true)
                            }
                            result
                        }
                        if (installed == null) {
                            terrainStalePlanCount += 1
                            terrainMissingReason = "terrain revision changed before install:${next.requestId}"
                            markTerrainInvalid()
                            continue
                        }
                        if (installed == TerrainMeshInstallResult.REJECTED) {
                            markTerrainInvalid()
                            continue
                        }
                        if (installed == TerrainMeshInstallResult.REPLACED) {
                            com.atsuishio.superbwarfare.Mod.LOGGER.debug(
                                "[tracked-terrain] {} installed #{} tris={} ramps={} walls={} region={}",
                                spec.vehicleId, next.requestId, next.triangleCount, next.ramps.size,
                                next.wallQuadCount, next.region,
                            )
                            terrainInstallCount += 1
                        }
                    }
                } finally {
                    // Clearing the flag and re-reading the slot must not lose a
                    // plan: a submit that arrived between `getAndSet` and this
                    // line saw `true` and did not queue a task of its own.
                    terrainApplyQueued.set(false)
                    terrainBuildQueued.set(false)
                    if (pendingTerrainPlan.get() != null && state.get() == State.ACTIVE) {
                        queueTerrainApplyIfNeeded()
                    }
                }
            }.whenComplete { _, error ->
                if (error != null) {
                    terrainApplyQueued.set(false)
                    terrainBuildQueued.set(false)
                    terrainMissingReason = "mesh apply failed: ${error.message}"
                    markTerrainInvalid()
                }
            }
        } catch (error: Throwable) {
            terrainApplyQueued.set(false)
            terrainBuildQueued.set(false)
            terrainMissingReason = "mesh submit failed: ${error.message}"
            markTerrainInvalid()
        }
    }

    /** True while the local mesh is being built or replaced. */
    fun terrainWaiting(): Boolean = !terrainReady

    /** Last terrain-cache revision the installed mesh was built from. */
    fun terrainRevision(): Long = terrainAppliedRevision

    /** Request id of the installed mesh, for diagnostics only. */
    fun terrainInstalledRequestId(): Long = terrainAppliedRequestId

    /**
     * Longitudinal corridor for this window.
     *
     * Nose and tail rises inside the track width may become ramps. Side rises
     * stay walls. The origin is the snapped window centre, not the live chassis
     * position: a few centimetres of travel must not change the corridor and
     * force another mesh swap. Heading is quantized inside [RampGate].
     */
    private fun rampGateFor(window: AABB): RampGate {
        val pose = publishedTerrainPose
        val lateral = maxOf(terrainEnvelope.halfX, 1.0) + 0.75
        return RampGate.forVehicle(
            (window.minX + window.maxX) * 0.5,
            (window.minZ + window.maxZ) * 0.5,
            pose?.forwardX ?: 0.0,
            pose?.forwardZ ?: 1.0,
            lateral,
        )
    }

    /** Conservative local sampling envelope; never a pass criterion. */
    fun terrainSamplingEnvelope(): TerrainEnvelope = terrainEnvelope

    /** Bodies currently factor-locked because the mesh is unavailable. */
    fun terrainHeldBodyCount(): Int = terrainWait.size

    private fun markTerrainInvalid() {
        terrainReady = false
        if (terrainBodiesHeld.get() || !terrainInvalidationQueued.compareAndSet(false, true)) return
        try {
            level.submitImmediateTask {
                try {
                    if (state.get() == State.ACTIVE && !terrainReady) {
                        terrainProxy.invalidate()
                        holdTerrainBodies()
                        terrainBodiesHeld.set(true)
                    }
                } finally {
                    terrainInvalidationQueued.set(false)
                }
            }
        } catch (_: Throwable) {
            // Level teardown already owns the bodies.
            terrainInvalidationQueued.set(false)
        }
    }

    private fun holdTerrainBodies() {
        level.assertPhysicsThread()
        if (terrainWait.isEmpty) terrainHoldCount += 1
        // Never kinematic: a tracked assembly already has mounts and run-end
        // joints, and Bullet rejects a joint between two kinematic bodies.
        terrainWait.holdAll(terrainBodies)
    }

    /** Register a body as part of the assembly and hold it if terrain is missing. */
    private fun trackTerrainBody(body: PhysicsRigidBody) {
        terrainBodies += body
        if (!terrainReady) {
            terrainWait.hold(body)
            terrainBodiesHeld.set(true)
        }
    }

    private fun releaseTerrainHold(restoreVelocity: Boolean) {
        level.assertPhysicsThread()
        terrainWait.releaseAll(terrainBodies, restoreVelocity)
        terrainBodiesHeld.set(false)
    }

    /** Number of dynamic wheel bodies currently registered, zero before create. */
    val wheelCount: Int get() = wheelBodies.values.sumOf { it.size }
    /** Number of live run runtimes, zero before create. */
    val runCount: Int get() = runtimes.values.sumOf { it.size }
    /** Number of external end constraints, zero before create. */
    val endJointCount: Int get() = endJoints.size

    /**
     * Native objects this runtime still owns, counted from the rollback ledger.
     *
     * Published maps can be cleared before removal finishes; the ledger is the
     * source of truth for "is anything still in the world".
     */
    val residualJointCount: Int get() = ledger.jointCount
    val residualBodyCount: Int get() = ledger.bodyCount
    val residualRuntimeCount: Int get() = ledger.runtimeCount
    val hasResidual: Boolean get() = ledger.hasRemaining

    /**
     * Assemble every native object.
     *
     * Stages are chained rather than nested because each Core call enqueues work
     * for the next physics task cycle; blocking on the physics thread would
     * deadlock the level.
     */
    fun create(): CompletableFuture<Void> {
        val result: CompletableFuture<Void>
        synchronized(this) {
            when (state.get()) {
                State.ACTIVE -> return CompletableFuture.completedFuture(null)
                State.CREATING -> return createFuture.get()
                    ?: CompletableFuture.failedFuture(IllegalStateException("create is in progress"))
                State.CLOSING, State.CLOSED, State.FAILED -> return CompletableFuture.failedFuture(
                    IllegalStateException("TrackedVehicleRuntime '${spec.vehicleId}' is closed"),
                )
                State.NEW -> state.set(State.CREATING)
            }
            // Published inside the lock so a concurrent close always observes the
            // future it must wait for instead of a half-created runtime.
            result = CompletableFuture()
            createFuture.set(result)
            createGeneration.set(generation.get())
        }

        // Stage 1: wheel bodies and mount constraints, on the physics thread.
        //
        // Every stage is flattened to `CompletableFuture<Void>` before the next
        // one is chained. Leaving a `CompletableFuture<CompletableFuture<...>>`
        // unflattened makes the chain advance while the inner work is still
        // queued, and the next stage then sees a half-built runtime.
        val bodiesStage: CompletableFuture<Void> = level
            .submitTask("tracked vehicle bodies ${spec.vehicleId}") {
                level.assertPhysicsThread()
                ensureCreating()
                // The chassis is caller-owned but must not fall while the first
                // local mesh is still being built. The previous raw AABB floor
                // existed immediately; a mesh needs one sampling cycle, and a
                // falling hull would meet it with a penetration impulse.
                trackTerrainBody(chassis)
                chassis.onResolvedContact { event -> noteTerrainSupport(SupportSource.CHASSIS, null, event) }
                val (origin, rotation) = chassisPose()
                val wheels = LinkedHashMap<TrackedSide, List<PhysicsRigidBody>>()
                val mounts = ArrayList<New6Dof>()
                for (side in TrackedSide.entries) {
                    val compiled = spec.side(side)
                    val created = ArrayList<PhysicsRigidBody>(compiled.wheels.size)
                    for (wheel in compiled.wheels) {
                        val body = createWheelBody(side, wheel, origin, rotation)
                        // Record before registration: a failing addBody must
                        // still leave the body reachable by rollback.
                        ledger.recordBody(body)
                        level.addBody(body)
                        created += body
                    }
                    wheels[side] = created
                    val byIndex = compiled.wheels.indices.associate { compiled.wheels[it].index to created[it] }
                    for (mount in compiled.mounts) {
                        val plan = mount.joint ?: continue
                        val wheelBody = byIndex.getValue(mount.wheelIndex)
                        val joint = TrackedJointFactory.create(plan, chassis, wheelBody) { created ->
                            ledger.recordJoint(created)
                        }
                        level.addJoint(joint)
                        mounts += joint
                    }
                }
                wheelBodies = wheels
                mountJoints = mounts
                rows = TrackedSide.entries.associateWith { side ->
                    spec.side(side).mounts.mapNotNull { mount ->
                        mount.springRow?.let { mount.wheelIndex to it }
                    }.toMap()
                }
            }
            .thenApply<Void> { null }

        // Stage 2: one TrackRunRuntime per run, placed at the chassis world pose.
        val runsStage: CompletableFuture<Void> = bodiesStage
            .thenCompose {
                level.submitTask("tracked vehicle runs ${spec.vehicleId}") {
                    level.assertPhysicsThread()
                    ensureCreating()
                    if (failCreateAfterBodies) {
                        failCreateAfterBodies = false
                        error("injected failure after wheel bodies")
                    }
                    val (origin, rotation) = chassisPose()
                    val created = LinkedHashMap<TrackedSide, List<TrackRunRuntime>>()
                    val futures = ArrayList<CompletableFuture<Void>>()
                    for (side in TrackedSide.entries) {
                        val compiled = spec.side(side)
                        val specs = TrackedRunSpecs.compileRuns(compiled, spec.vehicleId, spec.drivetrain.friction)
                        val list = ArrayList<TrackRunRuntime>(specs.size)
                        specs.forEach { local ->
                            val placed = place(local, origin, rotation)
                            val runtime = TrackRunRuntime(level, placed, host)
                            // Record before create(): a run that fails midway
                            // still has native links the ledger must close.
                            ledger.recordRuntime(runtime)
                            list += runtime
                            futures += runtime.create()
                        }
                        created[side] = list
                    }
                    runtimes = created
                    if (failCreateAfterRuns) {
                        failCreateAfterRuns = false
                        error("injected failure after runs")
                    }
                    ensureCreating()
                    CompletableFuture.allOf(*futures.toTypedArray())
                }
            }
            .thenCompose { it }

        // Stage 3: external run ends, inside withLinkBodies.
        val endsStage: CompletableFuture<Void> = runsStage
            .thenCompose {
                if (failCreateAfterEnds) {
                    failCreateAfterEnds = false
                    return@thenCompose CompletableFuture.failedFuture<Void>(
                        IllegalStateException("injected failure before end joints"),
                    )
                }
                val futures = ArrayList<CompletableFuture<*>>()
                for (side in TrackedSide.entries) {
                    val compiled = spec.side(side)
                    val list = runtimes[side] ?: error("no runtimes for $side")
                    val wheels = wheelBodies[side] ?: error("no wheels for $side")
                    val byIndex = compiled.wheels.indices.associate { compiled.wheels[it].index to wheels[it] }
                    compiled.runs.forEachIndexed { index, run ->
                        val runtime = list[index]
                        for (attachment in run.attachments) {
                            val plan = attachment.joint
                            futures += runtime.withLinkBodies { bodies ->
                                ensureCreating()
                                val runBody = bodies.single()
                                trackTerrainBody(runBody)
                                if (runSides.putIfAbsent(runBody, side) == null) {
                                    runBody.onResolvedContact { event ->
                                        val other = event.other
                                        val terrain =
                                            other.collisionGroup and TrackedTerrainCollision.TERRAIN_MESH != 0
                                        if (!terrain) return@onResolvedContact
                                        val snapshot = event.snapshot
                                        val impulse = snapshot.normalImpulse
                                        val accum = sideAccum.computeIfAbsent(side) { SideAccum() }
                                        if (impulse > 0f && snapshot.normal.y > 0.4) {
                                            noteRunSupport(
                                                side,
                                                impulse.toDouble(),
                                                tangentialForwardNs(snapshot.impulse, snapshot.normal),
                                            )
                                            accum.normal += impulse.toDouble()
                                            // Longitudinal ground traction only. A wall
                                            // hit is a real contact but its forward
                                            // impulse is resistance, not drive: including
                                            // it would throttle the vehicle for touching a
                                            // wall, which MCSP's per-slot traction does
                                            // not do.
                                            val forward = TrackedVehicleFrame.worldForward(
                                                chassis.getPhysicsRotation(null).toRotationMatrix(),
                                            )
                                            val impulseVec = snapshot.impulse
                                            accum.traction += impulseVec.x * forward.x +
                                                impulseVec.y * forward.y +
                                                impulseVec.z * forward.z
                                        }
                                    }
                                }
                                val bodyA = resolve(plan.bodyA, runBody, byIndex, side)
                                val bodyB = resolve(plan.bodyB, runBody, byIndex, side)
                                val joint = TrackedJointFactory.create(plan, bodyA, bodyB) { created ->
                                    ledger.recordJoint(created)
                                }
                                level.addJoint(joint)
                                synchronized(endJointLock) { endJoints = endJoints + joint }
                            }
                        }
                    }
                }
                CompletableFuture.allOf(*futures.toTypedArray())
            }
            .thenApply<Void> { null }

        endsStage.whenComplete { _, error ->
            if (error == null) {
                val committed = synchronized(this) {
                    // Register the listener in the same lock as the ACTIVE
                    // transition. A close that wins the lock first cannot miss it.
                    if (state.get() != State.CREATING || generation.get() != createGeneration.get()) {
                        false
                    } else {
                        state.set(State.ACTIVE)
                        ACTIVE_RUNTIMES += this
                        lastError = null
                        registerContactListener()
                        registerSupportWindow()
                        true
                    }
                }
                if (committed) {
                    result.complete(null)
                } else {
                    result.completeExceptionally(
                        IllegalStateException("TrackedVehicleRuntime '${spec.vehicleId}' closed before assembly committed"),
                    )
                }
            } else {
                lastError = error.message ?: error::class.java.simpleName
                // Roll back before reporting failure: a create that "failed"
                // must not leave wheels, runs or joints in the world.
                rollbackOnce().whenComplete { _, cleanupError ->
                    if (cleanupError != null) error.addSuppressed(cleanupError)
                    synchronized(this) {
                        ACTIVE_RUNTIMES -= this
                        clearPublishedState()
                        state.set(State.FAILED)
                    }
                    result.completeExceptionally(error)
                }
            }
            createFuture.set(null)
        }
        return result
    }

    /**
     * Roll back every native object this runtime created.
     *
     * Idempotent and shared by the create-failure path and [closeAsync], so a
     * failed assembly is torn down exactly once and a close that races a create
     * does not repeat the work.
     */
    private fun ensureCreating() {
        if (state.get() != State.CREATING || generation.get() != createGeneration.get()) {
            error("TrackedVehicleRuntime '${spec.vehicleId}' closed before this create stage committed")
        }
    }

    private fun rollbackOnce(): CompletableFuture<Void> {
        synchronized(this) {
            rollbackFuture.get()?.let { existing ->
                if (!existing.isCompletedExceptionally) return existing
            }
            val closeTerrain = level.submitTask("tracked terrain mesh close ${spec.vehicleId}") {
                level.terrainManager.releaseRegion(terrainRegionRequestId)
                terrainReady = false
                terrainWait.clear(dropMotion = true)
                terrainProxy.closeNow()
            }
            val future = closeTerrain.thenCompose { ledger.rollback() }
            rollbackFuture.set(future)
            return future
        }
    }

    private fun clearPublishedState() {
        pendingTerrainPlan.set(null)
        requestedTerrainWindow = null
        installedTerrainWindow = null
        installedTerrainRevision = null
        installedRampBucket = Int.MIN_VALUE
        publishedTerrainPose = null
        publishedRender = null
        renderPhase.fill(0.0)
        renderTravel.fill(0.0)
        renderPhaseSeeded.fill(false)
        wheelBodies = emptyMap()
        mountJoints = emptyList()
        runtimes = emptyMap()
        synchronized(endJointLock) { endJoints = emptyList() }
        rows = emptyMap()
        runSides.clear()
        beltSupported = false
        sideAccum.clear()
        sideWindows = emptyMap()
        sideSupport.values.forEach { it.reset() }
        chassisSupportCount = 0
        chassisSupportImpulse = 0.0
        supportContacts = SupportContactWindow.EMPTY
        terrainWait.clear(dropMotion = false)
        terrainBodiesHeld.set(false)
        terrainBodies.clear()
    }

    /**
     * Move the whole assembly to a new chassis pose.
     *
     * Used by spawn placement, NBT reload and teleport. The chassis itself is
     * the caller's; this method only re-seats the running gear relative to it so
     * the mounts and run ends keep describing the same relative mechanism.
     *
     * Wheels and runs are teleported, not simulated there: a reset must not
     * leave the assembly half-solved at the old pose.
     */
    fun reset(): CompletableFuture<Void> {
        if (state.get() != State.ACTIVE) {
            return CompletableFuture.failedFuture(IllegalStateException("runtime is not active"))
        }
        if (level.isPhysicsThread()) {
            return try {
                resetNow()
                CompletableFuture.completedFuture(null)
            } catch (error: Throwable) {
                CompletableFuture.failedFuture(error)
            }
        }
        val future = CompletableFuture<Void>()
        level.submitTask("tracked vehicle reset ${spec.vehicleId}") {
            resetNow()
        }.whenComplete { _, error ->
            if (error == null) future.complete(null) else future.completeExceptionally(error)
        }
        return future
    }

    /**
     * Re-seat wheels and runs at the current hull pose. Physics thread only.
     *
     * The hull must already be at the destination. This does not submit another
     * task, so a caller that has just moved the hull cannot observe a mixed pose.
     */
    fun resetNow() {
        level.assertPhysicsThread()
        if (state.get() != State.ACTIVE) return
        val wheels = wheelBodies
        val currentRuntimes = runtimes
        val (origin, rotation) = chassisPose()
            // The hull's own motion must propagate into the running gear, not be
            // discarded: a teleport that keeps the hull moving but zeroes every
            // wheel and run would inject a step change in the constraints.
            val hullVelocity = chassis.getLinearVelocity(null)
            val hullAngular = chassis.getAngularVelocity(null)
            val rotationMatrix = rotation.toRotationMatrix()
            for (side in TrackedSide.entries) {
                val compiled = spec.side(side)
                val bodies = wheels[side] ?: continue
                compiled.wheels.forEachIndexed { index, wheel ->
                    val body = bodies[index]
                    val local = Vector3f(
                        wheel.assemblyCenter.x.toFloat(),
                        wheel.assemblyCenter.y.toFloat(),
                        wheel.assemblyCenter.z.toFloat(),
                    )
                    val worldOffset = rotationMatrix.mult(local, Vector3f())
                    body.clearForces()
                    body.setPhysicsLocation(origin.add(worldOffset))
                    body.setPhysicsRotation(rotation)
                    body.setLinearVelocity(hullVelocity.add(hullAngular.cross(worldOffset, Vector3f())))
                    body.setAngularVelocity(hullAngular)
                    body.activate(true)
                }
                // Runs are re-seated through the Core runtime so its snapshot and
                // pose epoch stay authoritative rather than being written behind
                // its back.
                currentRuntimes[side]?.forEachIndexed { index, runtime ->
                    val run = compiled.runs[index]
                    val offset = Vector3f(
                        run.center.x.toFloat(),
                        run.center.y.toFloat(),
                        run.center.z.toFloat(),
                    )
                    val worldOffset = rotationMatrix.mult(offset, Vector3f())
                    val world = origin.add(worldOffset)
                    val combined = Quaternion(rotation).mult(
                        Quaternion(run.rotation.x, run.rotation.y, run.rotation.z, run.rotation.w),
                    )
                    combined.normalizeLocal()
                    runtime.resetNow(
                        listOf(
                            cn.solarmoon.spark_core.physics.track.TrackRunLinkFrame(
                                position = Vec3(world.x.toDouble(), world.y.toDouble(), world.z.toDouble()),
                                rotationX = combined.x,
                                rotationY = combined.y,
                                rotationZ = combined.z,
                                rotationW = combined.w,
                                linearVelocity = Vec3(
                                    hullVelocity.x.toDouble() + hullAngular.y * worldOffset.z - hullAngular.z * worldOffset.y,
                                    hullVelocity.y.toDouble() + hullAngular.z * worldOffset.x - hullAngular.x * worldOffset.z,
                                    hullVelocity.z.toDouble() + hullAngular.x * worldOffset.y - hullAngular.y * worldOffset.x,
                                ),
                                angularVelocity = Vec3(
                                    hullAngular.x.toDouble(),
                                    hullAngular.y.toDouble(),
                                    hullAngular.z.toDouble(),
                                ),
                                active = true,
                                colliding = false,
                            ),
                        ),
                    )
                }
            }
            // The reset future must not report success until every run has
            // actually been re-seated; otherwise a caller that immediately reads
            // the assembly can observe half-reset runs.
            appliedDriveN = 0f
    }

    /**
     * Apply every mount force for one substep: suspension rows, tensioner
     * preload/damping, and nothing for a fixed mount.
     *
     * Must be called on the physics thread before the solver runs. Two details
     * matter for correctness:
     *
     * - the compression rate is the **relative** rate at the mount, so the
     *   chassis's own rotation about its centre is included. Using the hull's
     *   centre velocity alone would read a pitching vehicle as compressing every
     *   station at once.
     * - a tensioned idler is a real mechanism. The compiled preload and damping
     *   are applied along the authored travel axis; a fixed sprocket or roller
     *   gets no invented spring.
     */
    fun applySuspension() {
        if (state.get() != State.ACTIVE) return
        level.assertPhysicsThread()
        val wheels = wheelBodies
        val currentRows = rows
        val chassisVelocity = chassis.getLinearVelocity(null)
        val chassisAngular = chassis.getAngularVelocity(null)
        val chassisLocation = chassis.getPhysicsLocation(null)
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val bodies = wheels[side] ?: continue
            val sideRows = currentRows[side] ?: continue
            compiled.wheels.forEachIndexed { index, wheel ->
                val body = bodies[index]
                val mount = compiled.mounts.firstOrNull { it.wheelIndex == wheel.index } ?: return@forEachIndexed
                if (mount.kind != WheelMountKind.PRISMATIC) return@forEachIndexed
                val axis = rowAxis(wheel)
                // Velocity of the hull at this wheel's anchor, not at its centre.
                val anchorOffset = body.getPhysicsLocation(null).subtract(chassisLocation)
                val hullAtWheel = chassisVelocity.add(chassisAngular.cross(anchorOffset, Vector3f()))
                val relative = body.getLinearVelocity(null).subtract(hullAtWheel).dot(axis)

                val row = sideRows[wheel.index]
                if (row != null) {
                    val travel = travelOf(wheel, body)
                    val force = PhysicsSuspensionRow.force(row, SuspensionRowState(travel, relative))
                    applyMountForce(chassis, body, axis, force, body.getPhysicsLocation(null))
                    recordTelemetry(wheel.index, travel, force)
                } else {
                    // Tensioned idler: preload pushes along the travel axis and
                    // damping opposes the relative rate. Sign follows the mount
                    // axis, which the compiler derived from authored data.
                    val preload = mount.preloadN?.toFloat() ?: return@forEachIndexed
                    val damping = mount.dampingNsPerM?.toFloat() ?: 0f
                    val force = preload - damping * relative
                    if (force.isFinite()) {
                        applyMountForce(chassis, body, axis, force, body.getPhysicsLocation(null))
                        recordTelemetry(wheel.index, travelOf(wheel, body), force)
                    }
                }
            }
        }
    }

    /** Drain the telemetry recorded since the last call. */
    fun drainTelemetry(): List<SuspensionSample> {
        val out = ArrayList<SuspensionSample>()
        while (true) out += telemetry.poll() ?: break
        return out
    }

    /**
     * Remove every native object this runtime created, in reverse order.
     *
     * External joints first (they reference both a run and a wheel), then the
     * run runtimes, then the mount constraints, then the wheel bodies. The
     * chassis belongs to the caller and is never touched.
     *
     * The teardown is the same [ledger] rollback the create-failure path uses,
     * so a close racing a create cannot remove half of an assembly twice. A
     * create that is still queued is cancelled by the state transition; once it
     * fails it rolls itself back. The returned future completes only after the
     * last native object is actually gone.
     */
    fun closeAsync(): CompletableFuture<Void> {
        synchronized(this) {
            closeFuture.get()?.let { if (!it.isDone) return it }
            val result = CompletableFuture<Void>()
            closeFuture.set(result)
            generation.incrementAndGet()
            level.terrainManager.releaseRegion(terrainRegionRequestId)
            unregisterContactListener()
            supportWindow?.dispose()
            supportWindow = null
            when (state.get()) {
                State.CLOSED -> {
                    result.complete(null)
                    return result
                }
                State.NEW -> {
                    state.set(State.CLOSED)
                    result.complete(null)
                    return result
                }
                State.ACTIVE -> {
                    ACTIVE_RUNTIMES -= this
                    state.set(State.CLOSING)
                    // Published views are dropped *after* teardown decided what
                    // to remove; clearing them early would hide objects from the
                    // ledger and let a failed removal look like success.
                }
                State.CREATING -> {
                    ACTIVE_RUNTIMES -= this
                    // The create chain keeps running, but it cannot commit
                    // against a closing state and will roll itself back. Wait
                    // for that before tearing the ledger down a second time.
                    state.set(State.CLOSING)
                }
                State.CLOSING, State.FAILED -> {
                    // A failed close or a partially failed create may still own
                    // native objects; a retry must be able to remove them.
                    state.set(State.CLOSING)
                }
            }

            val pendingCreate = createFuture.get()
            val settled = if (pendingCreate != null) {
                // Mirror the create's own exception so a waiter observes the
                // original failure rather than an unrelated cancellation.
                pendingCreate.handle { _, _ -> null }
            } else {
                CompletableFuture.completedFuture(null)
            }

            val teardown = settled.thenCompose { rollbackOnce() }
            teardown.whenComplete { _, error ->
                synchronized(this) {
                    clearPublishedState()
                    if (error == null) {
                        state.set(State.CLOSED)
                        lastError = null
                        result.complete(null)
                    } else {
                        state.set(State.FAILED)
                        lastError = error.message ?: error::class.java.simpleName
                        result.completeExceptionally(error)
                    }
                }
            }
            return result
        }
    }

    override fun close() {
        closeAsync()
    }

    /**
     * Remove this assembly on the physics thread without queueing another task.
     *
     * Level shutdown calls this from a shutdown participant. [closeAsync] cannot
     * be used there: admission is already closed, so a submitted removal is
     * rejected and must not be reported as a finished close.
     */
    fun closeNow() {
        level.assertPhysicsThread()
        level.terrainManager.releaseRegion(terrainRegionRequestId)
        synchronized(this) {
            generation.incrementAndGet()
            unregisterContactListener()
            supportWindow?.dispose()
            supportWindow = null
            if (state.get() == State.CLOSED && !ledger.hasRemaining) {
                closeFuture.get()?.let { if (!it.isDone) it.complete(null) }
                return
            }
            ACTIVE_RUNTIMES -= this
            state.set(State.CLOSING)
        }
        try {
            terrainReady = false
            terrainWait.clear(dropMotion = true)
            terrainProxy.closeNow()
            ledger.rollbackNow()
            synchronized(this) {
                clearPublishedState()
                state.set(State.CLOSED)
                lastError = null
                val existing = closeFuture.get()
                if (existing == null) closeFuture.set(CompletableFuture.completedFuture(null))
                else if (!existing.isDone) existing.complete(null)
            }
        } catch (error: Throwable) {
            synchronized(this) {
                state.set(State.FAILED)
                lastError = error.message ?: error::class.java.simpleName
                closeFuture.get()?.let { if (!it.isDone) it.completeExceptionally(error) }
            }
            throw error
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun chassisPose(): Pair<Vector3f, Quaternion> =
        chassis.getPhysicsLocation(null).clone() to chassis.getPhysicsRotation(null).clone()

    private fun createWheelBody(
        side: TrackedSide,
        wheel: CompiledWheel,
        origin: Vector3f,
        rotation: Quaternion,
    ): PhysicsRigidBody {
        val shape = CylinderCollisionShape(
            Vector3f(wheel.halfWidth.toFloat(), wheel.radius.toFloat(), wheel.radius.toFloat()),
            PhysicsSpace.AXIS_X,
        ).apply { margin = .001f }
        val local = Vector3f(
            wheel.assemblyCenter.x.toFloat(),
            wheel.assemblyCenter.y.toFloat(),
            wheel.assemblyCenter.z.toFloat(),
        )
        val world = origin.add(rotation.toRotationMatrix().mult(local, Vector3f()))
        return PhysicsRigidBody(shape, wheel.massKg.toFloat()).apply {
            name = "sbw_track_${spec.vehicleId}_${side.name.lowercase()}_wheel_${wheel.index}"
            owner = host
            collisionGroup = CollisionGroups.WHEEL
            // The private terrain mesh is allowed again. Removing it before the
            // run boxes were shown to carry 99A let the solid hull boxes rest on
            // the mesh, about half a metre below the road-wheel bottoms, which
            // is the sunk hull. This is diagnostic protection, not the final
            // load path: runTerrain must still be proven before wheel-terrain
            // contact can be removed. Raw block AABB terrain stays excluded.
            setCollideWithGroups(TrackedTerrainCollision.TERRAIN_MESH or CollisionGroups.TRACK_LINK)
            collideWithOwnerGroups = CollisionGroups.TRACK_LINK
            friction = spec.drivetrain.friction.toFloat()
            restitution = 0f
            setDamping(.05f, .1f)
            // A zero threshold treats any numerical noise as motion, so the
            // wheel can never sleep and the suspension keeps the hull twitching.
            // 0.02 is below the chassis threshold and still lets a real shove
            // wake the wheel.
            setSleepingThresholds(0.02f, 0.02f)
            // Explicit gravity, not a protected zero: the runtime is the only
            // owner of this body's gravity and it must match the hull's.
            setProtectGravity(true)
            setGravity(gravity)
            isContactResponse = true
            setPhysicsLocation(world)
            setPhysicsRotation(rotation)
        }.also {
            trackTerrainBody(it)
            it.onResolvedContact { event -> noteTerrainSupport(SupportSource.WHEEL, side, event) }
        }
    }

    /**
     * Run gravity used by the running gear, as a native vector.
     *
     * The production caller passes the same value its hull integration uses.
     * [TrackedRunSpec.explicitGravity] is set from it so a run never silently
     * inherits the world gravity while its wheels are protected.
     */
    private fun runGravity(): Vec3 = Vec3(gravity.x.toDouble(), gravity.y.toDouble(), gravity.z.toDouble())

    /** Transform one chassis-local run spec into world space. */
    private fun place(local: TrackRunSpec, origin: Vector3f, rotation: Quaternion): TrackRunSpec {
        val links = local.links.map { link ->
            val offset = Vector3f(
                link.position.x.toFloat(),
                link.position.y.toFloat(),
                link.position.z.toFloat(),
            )
            val world = origin.add(rotation.toRotationMatrix().mult(offset, Vector3f()))
            val combined = Quaternion(rotation).mult(
                Quaternion(link.rotationX, link.rotationY, link.rotationZ, link.rotationW),
            )
            combined.normalizeLocal()
            TrackRunLinkSpec(
                massKg = link.massKg,
                shape = link.shape,
                position = Vec3(world.x.toDouble(), world.y.toDouble(), world.z.toDouble()),
                rotationX = combined.x,
                rotationY = combined.y,
                rotationZ = combined.z,
                rotationW = combined.w,
            )
        }
        return TrackRunSpec(
            name = local.name,
            links = links,
            joints = local.joints,
            collisionGroup = local.collisionGroup,
            collideWithGroups = local.collideWithGroups,
            collideWithOwnerGroups = local.collideWithOwnerGroups,
            linearDamping = local.linearDamping,
            angularDamping = local.angularDamping,
            friction = local.friction,
            restitution = local.restitution,
            continuousCollision = local.continuousCollision,
            // Preserve whatever the adapter declared, but the production path
            // always supplies the runtime's gravity so wheels and runs agree.
            explicitGravity = local.explicitGravity ?: runGravity(),
            maxLinkForceN = local.maxLinkForceN,
            maxLinkTorqueImpulseNms = local.maxLinkTorqueImpulseNms,
        )
    }

    private fun resolve(
        ref: CompiledBodyRef,
        runBody: PhysicsRigidBody,
        wheels: Map<Int, PhysicsRigidBody>,
        side: TrackedSide,
    ): PhysicsRigidBody = when (ref.kind) {
        CompiledBodyKind.WHEEL -> wheels.getValue(ref.ordinal)
        CompiledBodyKind.RUN -> runBody
        CompiledBodyKind.CHASSIS -> error("a run end cannot reference the chassis ($side)")
    }

    /**
     * Signed compression travel of one station, metres.
     *
     * Measured in the mount constraint's own frame so the value means the same
     * thing as the B2 fixtures' `travelOf`: positive toward the chassis.
     */
    private fun travelOf(wheel: CompiledWheel, body: PhysicsRigidBody): Float {
        val plan = spec.side(wheel.side).mount(wheel.index).joint ?: return 0f
        val chassisInverse = chassis.getPhysicsRotation(null).inverse().toRotationMatrix()
        val relative = body.getPhysicsLocation(null).subtract(chassis.getPhysicsLocation(null))
        val inChassisFrame = chassisInverse.mult(relative, Vector3f())
        val anchor = Vector3f(
            plan.frameInA.origin.x.toFloat(),
            plan.frameInA.origin.y.toFloat(),
            plan.frameInA.origin.z.toFloat(),
        )
        val fromAnchor = inChassisFrame.subtract(anchor)
        val frameInverse = Quaternion(
            plan.frameInA.rotation.x,
            plan.frameInA.rotation.y,
            plan.frameInA.rotation.z,
            plan.frameInA.rotation.w,
        ).normalizeLocal().inverse().toRotationMatrix()
        return frameInverse.mult(fromAnchor, Vector3f()).y
    }

    /** The row's axis in world space, from the mount's own frame. */
    private fun rowAxis(wheel: CompiledWheel): Vector3f {
        val plan = spec.side(wheel.side).mount(wheel.index).joint
            ?: return chassis.getPhysicsRotation(null).toRotationMatrix().mult(Vector3f(0f, 1f, 0f), Vector3f())
        val local = Quaternion(
            plan.frameInA.rotation.x,
            plan.frameInA.rotation.y,
            plan.frameInA.rotation.z,
            plan.frameInA.rotation.w,
        ).normalizeLocal().toRotationMatrix().mult(Vector3f(0f, 1f, 0f), Vector3f())
        return chassis.getPhysicsRotation(null).toRotationMatrix().mult(local, Vector3f()).normalize()
    }

    private fun recordTelemetry(wheelIndex: Int, travel: Float, force: Float) {
        // Bounded: telemetry must never become an unbounded leak on a long-lived
        // vehicle. A caller that wants every sample drains it each step.
        if (telemetry.size > 4096) return
        telemetry += SuspensionSample(wheelIndex, travel, force, level.tickCount)
    }

    /** One suspension reading, for diagnostics and tests. */
    data class SuspensionSample(
        val wheelIndex: Int,
        val travelM: Float,
        val forceN: Float,
        val tick: Long,
    )

    // ------------------------------------------------------- visible gear snapshot

    private val renderPhase = doubleArrayOf(0.0, 0.0)
    private val renderTravel = doubleArrayOf(0.0, 0.0)
    private val renderPhaseSeeded = booleanArrayOf(false, false)
    @Volatile private var publishedRender: TrackedRenderState? = null

    fun latestRenderState(): TrackedRenderState? = publishedRender

    /**
     * Copy the wheel centres and belt phase for the renderer.
     *
     * Physics thread only. The result contains no native handle. Visible plate
     * count is fixed from the first solvable envelope; later suspension changes
     * only the path, not the plate count.
     */
    fun captureRenderState(frame: Long): TrackedRenderState? {
        level.assertPhysicsThread()
        if (state.get() != State.ACTIVE) return publishedRender
        val measured = beltSurfaceSpeeds()
        val dt = 1.0 / PhysicsLevel.TPS
        val left = captureSide(TrackedSide.LEFT, measured.first, dt) ?: return publishedRender
        val right = captureSide(TrackedSide.RIGHT, measured.second, dt) ?: return publishedRender
        val snapshot = TrackedRenderState(
            frame = frame,
            originX = spec.chassisOriginX,
            originY = spec.chassisOriginY,
            originZ = spec.chassisOriginZ,
            renderScale = spec.renderScale,
            left = left,
            right = right,
        )
        publishedRender = snapshot
        return snapshot
    }

    private fun captureSide(
        side: TrackedSide,
        measuredSpeed: Double,
        dt: Double,
    ): TrackedSideRenderState? {
        val compiled = spec.side(side)
        val bodies = wheelBodies[side] ?: return null
        if (bodies.size != compiled.wheels.size) return null
        val inverse = chassis.getPhysicsRotation(null).inverse().toRotationMatrix()
        val origin = chassis.getPhysicsLocation(null)
        val byIndex = HashMap<Int, Pair<CompiledWheel, PhysicsRigidBody>>(compiled.wheels.size)
        compiled.wheels.forEachIndexed { index, wheel ->
            byIndex[wheel.index] = wheel to bodies[index]
        }
        val template = spec.renders[side]
        val count = template?.visibleCount ?: 0
        val rest = template?.restPitchM ?: 0.0
        val ordinal = if (side == TrackedSide.LEFT) 0 else 1
        // Measured run velocity, never the commanded target. A target keeps the
        // visible belt and wheels turning while the hull is stuck, which hides
        // a missing contact. Coast and brake already used this value.
        val speed = measuredSpeed
        if (!renderPhaseSeeded[ordinal]) {
            renderPhase[ordinal] = template?.initialPhase ?: 0.0
            renderPhaseSeeded[ordinal] = true
        }
        if (count > 0 && rest > 1.0e-6 && speed.isFinite() && dt.isFinite()) {
            // Phase advances in fixed rest-pitch units. Plate spacing uses the
            // current path length, matching MCSP's linkS.
            renderPhase[ordinal] = wrapPhase(renderPhase[ordinal] + speed * dt / rest, count.toDouble())
            renderTravel[ordinal] += speed * dt
        }
        val travel = renderTravel[ordinal]
        val poses = ArrayList<TrackedWheelRenderPose>(compiled.members.size)
        for (member in compiled.members) {
            val (wheel, body) = byIndex[member.wheelIndex] ?: return null
            val local = inverse.mult(body.getPhysicsLocation(null).subtract(origin), Vector3f())
            val suspended = wheel.isSuspended
            val suspension = if (suspended) travelOf(wheel, body).toDouble() else 0.0
            val tension = tensionTravel(wheel, local)
            val spin = if (wheel.radius > 1.0e-4 && travel.isFinite()) {
                wrapAngle(-travel / wheel.radius)
            } else {
                0.0
            }
            poses += TrackedWheelRenderPose(
                index = wheel.index,
                bone = wheel.bone,
                x = local.x.toDouble(),
                y = local.y.toDouble(),
                z = local.z.toDouble(),
                radius = wheel.radius,
                outside = member.wrap == TrackWrap.OUTSIDE,
                suspended = suspended,
                suspension = suspension,
                tension = tension,
                spin = spin,
            )
        }
        return TrackedSideRenderState(
            phase = renderPhase[ordinal],
            beltSpeed = if (speed.isFinite()) speed else 0.0,
            planeX = compiled.belt.centerPlaneX,
            visibleCount = count,
            restPitch = rest,
            templateBone = template?.linkTemplateBone.orEmpty(),
            thickness = compiled.belt.thicknessM,
            seatAlong = template?.seatAlongM ?: 0.0,
            seatNormal = template?.seatNormalM ?: 0.0,
            seatAngle = template?.seatAngleRad ?: 0.0,
            wheels = poses,
        )
    }

    private fun tensionTravel(wheel: CompiledWheel, local: Vector3f): Double {
        val axis = wheel.tensioner?.axis ?: return 0.0
        val length = axis.length()
        if (length < 1.0e-6) return 0.0
        val dx = local.x.toDouble() - wheel.bindCenter.x
        val dy = local.y.toDouble() - wheel.bindCenter.y
        val dz = local.z.toDouble() - wheel.bindCenter.z
        return (dx * axis.x + dy * axis.y + dz * axis.z) / length
    }

    private fun wrapPhase(value: Double, cycle: Double): Double {
        if (cycle <= 0.0 || !value.isFinite()) return 0.0
        var result = value % cycle
        if (result < 0.0) result += cycle
        return result
    }

    private fun wrapAngle(value: Double): Double {
        if (!value.isFinite()) return 0.0
        val cycle = Math.PI * 2.0
        var result = value % cycle
        if (result > Math.PI) result -= cycle
        if (result <= -Math.PI) result += cycle
        return result
    }

    // ------------------------------------------------------------------ S4 drive

    /** Drive demand for one substep, in normalized units. */
    data class DriveCommand(
        /** -1 reverse .. +1 forward, the centre demand shared by both sides. */
        val throttle: Float = 0f,
        /** -1 left .. +1 right. Positive steers right, matching the entity input. */
        val steer: Float = 0f,
        /** True while the driver is braking rather than coasting. */
        val brake: Boolean = false,
        /** False when no driver is aboard: the vehicle coasts. */
        val powered: Boolean = false,
    ) {
        companion object {
            val COAST = DriveCommand()
        }
    }

    /**
     * MCSP-style stateful response layer. Kept separate from the stateless
     * [TrackedDriveController.resolve] so the control contract can still be
     * tested in isolation; it only carries the steer lever and per-side slew.
     */
    private val driveController = TrackedDriveController.Controller()

    @Volatile private var driveCommand = DriveCommand.COAST
    @Volatile private var lastLeftDemand = 0.0
    @Volatile private var lastRightDemand = 0.0
    @Volatile private var lastSpeedMps = 0.0
    /**
     * Control quantity requested during the last [applyDrive], newtons.
     *
     * **Not** a measurement of traction: Bullet clears applied forces at the end
     * of every substep, so this is the value the controller asked the contact
     * drive layer for, not a native read-back and not proof the ground carried it.
     */
    @Volatile private var appliedDriveN = 0f
    /** Last control demand resolved by [applyDrive]; diagnostics and tests. */
    @Volatile private var lastDemand: TrackedDriveController.Demand = TrackedDriveController.Demand.COAST

    /** Last computed per-side demand, for diagnostics. -1..+1 each. */
    fun lastDriveDemand(): Pair<Double, Double> = lastLeftDemand to lastRightDemand
    /** Last measured forward speed, m/s. */
    fun lastSpeed(): Double = lastSpeedMps
    /** Last resolved control demand; never a force measurement. */
    fun lastDemand(): TrackedDriveController.Demand = lastDemand

    /**
     * Publish this substep's drive demand. Cheap and thread-safe: the actual
     * forces are computed in [applyDrive] on the physics thread.
     */
    fun setDriveCommand(command: DriveCommand) {
        driveCommand = command
    }

    /**
     * Control quantity requested by the last [applyDrive], newtons.
     *
     * Positive is forward. This is the value handed to the drive layer, **not** a
     * native read-back: Bullet clears applied forces at the end of every substep,
     * and a fixed wheel-end body does not translate freely, so neither
     * `totalAppliedForce` nor belt speed can prove traction. Kept for control
     * diagnostics only.
     */
    fun driveForceN(): Float = appliedDriveN

    /**
     * One physics substep of running-gear work: suspension and drive.
     *
     * Production calls the three pieces from `VehicleBody.preStep` because that
     * is where the hull's own pre-step hook already lives. This wrapper exists so
     * a runtime created without a `VehicleBody` (a test, or a future non-entity
     * owner) can still be stepped explicitly. It must run on the physics thread.
     */
    fun step() {
        if (state.get() != State.ACTIVE) return
        level.assertPhysicsThread()
        applySuspension()
        applyDrive()
    }

    /**
     * Publish the chassis centre for the game-thread terrain sampler.
     *
     * Physics thread only. The game thread must never read a Bullet body just
     * to ask where the local terrain window is.
     */
    private data class TerrainPose(
        val center: Vec3,
        val forwardX: Double,
        val forwardZ: Double,
    )
    @Volatile private var publishedTerrainPose: TerrainPose? = null
    /** Centres of the running gear, for ramp rejection. Not a collision shape. */
    @Volatile private var publishedGearProbes: List<GearProbe> = emptyList()
    /** Hull parts used only to draw the installed chassis OBBs. */
    @Volatile private var debugCollisionParts: List<CollisionPart> = emptyList()
    @Volatile private var collisionDebug: TrackedCollisionDebug.Snapshot? = null
    private val collisionDebugEpoch = java.util.concurrent.atomic.AtomicLong(0)

    fun gearProbes(): List<GearProbe> = publishedGearProbes

    fun collisionDebugSnapshot(): TrackedCollisionDebug.Snapshot? = collisionDebug

    internal fun setDebugCollisionParts(parts: List<CollisionPart>) {
        debugCollisionParts = parts
    }

    fun publishStepPose() {
        level.assertPhysicsThread()
        if (state.get() != State.ACTIVE) return
        val location = chassis.getPhysicsLocation(null)
        val forward = TrackedVehicleFrame.worldForward(chassis.getPhysicsRotation(null).toRotationMatrix())
        val previous = publishedTerrainPose
        val fx = forward.x.toDouble()
        val fz = forward.z.toDouble()
        val flat = if (fx * fx + fz * fz < 1.0e-8) {
            (previous?.forwardX ?: 0.0) to (previous?.forwardZ ?: 1.0)
        } else {
            fx to fz
        }
        publishedTerrainPose = TerrainPose(
            Vec3(location.x.toDouble(), location.y.toDouble(), location.z.toDouble()),
            flat.first,
            flat.second,
        )
        publishGearProbes()
        if (TrackedTerrainDebugService.watchingAnyone()) publishCollisionDebug()
    }

    /**
     * Apply a suspension pair without resetting an already-active body's sleep timer.
     *
     * The shared paired-force helper calls `activate(true)` on every use. That
     * resets Bullet's deactivation timer, so a static spring force keeps the
     * hull and every road wheel awake forever. Waking only a sleeping body
     * still starts a parked vehicle when the driver asks it to move.
     */
    private fun applyMountForce(
        hull: PhysicsRigidBody,
        wheel: PhysicsRigidBody,
        upWorld: Vector3f,
        forceN: Float,
        anchorWorld: Vector3f,
    ) {
        if (!forceN.isFinite() || abs(forceN) < 1.0e-4f) return
        if (!upWorld.x.isFinite() || !upWorld.y.isFinite() || !upWorld.z.isFinite()) return
        if (!anchorWorld.x.isFinite() || !anchorWorld.y.isFinite() || !anchorWorld.z.isFinite()) return
        val up = Vector3f(upWorld)
        val length = up.length()
        if (length < 1.0e-6f) up.set(0f, 1f, 0f) else up.multLocal(1f / length)
        val force = up.mult(forceN, Vector3f())
        val relative = Vector3f(anchorWorld).subtractLocal(hull.getPhysicsLocation(null))
        hull.applyForce(force, relative)
        wheel.applyCentralForce(force.mult(-1f, Vector3f()))
        if (!hull.isActive) hull.activate(true)
        if (!wheel.isActive) wheel.activate(true)
    }

    /** Physics thread. Replaces the published list; never mutates a list a builder already holds. */
    private fun publishGearProbes() {
        val probes = ArrayList<GearProbe>()
        val (origin, rotation) = chassisPose()
        val matrix = rotation.toRotationMatrix()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val bodies = wheelBodies[side].orEmpty()
            compiled.wheels.forEachIndexed { index, wheel ->
                val body = bodies.getOrNull(index) ?: return@forEachIndexed
                val location = body.getPhysicsLocation(null)
                val bodyRotation = body.getPhysicsRotation(null)
                val half = TrackedCollisionDebug.worldHalf(
                    bodyRotation,
                    Vector3f(wheel.halfWidth.toFloat(), wheel.radius.toFloat(), wheel.radius.toFloat()),
                )
                probes += GearProbe(
                    location.x.toDouble(), location.y.toDouble(), location.z.toDouble(),
                    half.x.toDouble(), half.y.toDouble(), half.z.toDouble(),
                )
            }
            val runs = runtimes[side].orEmpty()
            compiled.runs.forEachIndexed { index, run ->
                val frame = runs.getOrNull(index)?.snapshot()?.links?.firstOrNull()?.takeIf { it.finite() }
                val center: Vector3f
                val bodyRotation: Quaternion
                if (frame != null) {
                    center = Vector3f(
                        frame.position.x.toFloat(),
                        frame.position.y.toFloat(),
                        frame.position.z.toFloat(),
                    )
                    bodyRotation = Quaternion(frame.rotationX, frame.rotationY, frame.rotationZ, frame.rotationW)
                } else {
                    val local = Vector3f(run.center.x.toFloat(), run.center.y.toFloat(), run.center.z.toFloat())
                    center = matrix.mult(local, Vector3f()).addLocal(origin)
                    bodyRotation = Quaternion(rotation).mult(
                        Quaternion(run.rotation.x, run.rotation.y, run.rotation.z, run.rotation.w),
                    )
                }
                val half = TrackedCollisionDebug.worldHalf(
                    bodyRotation,
                    Vector3f(run.halfExtents.x.toFloat(), run.halfExtents.y.toFloat(), run.halfExtents.z.toFloat()),
                )
                probes += GearProbe(
                    center.x.toDouble(), center.y.toDouble(), center.z.toDouble(),
                    half.x.toDouble(), half.y.toDouble(), half.z.toDouble(),
                )
            }
        }
        publishedGearProbes = probes
    }

    /**
     * Physics thread. Copies the installed hull, wheel and run shapes into a
     * plain edge list. Opening the view does not activate a body.
     */
    private fun publishCollisionDebug() {
        val segments = ArrayList<Float>(256)
        val (origin, rotation) = chassisPose()
        val matrix = rotation.toRotationMatrix()
        for (part in debugCollisionParts) {
            val local = Vector3f(part.center.x.toFloat(), part.center.y.toFloat(), part.center.z.toFloat())
            val world = matrix.mult(local, Vector3f()).addLocal(origin)
            val worldRotation = Quaternion(rotation).mult(
                Quaternion(part.rotation.x, part.rotation.y, part.rotation.z, part.rotation.w),
            )
            TrackedCollisionDebug.appendBox(
                segments,
                world,
                worldRotation,
                Vector3f(part.half.x.toFloat(), part.half.y.toFloat(), part.half.z.toFloat()),
                TrackedCollisionDebug.KIND_HULL,
            )
        }
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val bodies = wheelBodies[side].orEmpty()
            compiled.wheels.forEachIndexed { index, wheel ->
                val body = bodies.getOrNull(index) ?: return@forEachIndexed
                TrackedCollisionDebug.appendCylinder(
                    segments,
                    body.getPhysicsLocation(null),
                    body.getPhysicsRotation(null),
                    wheel.halfWidth.toFloat(),
                    wheel.radius.toFloat(),
                    TrackedCollisionDebug.KIND_WHEEL,
                )
            }
            val runs = runtimes[side].orEmpty()
            compiled.runs.forEachIndexed { index, run ->
                val frame = runs.getOrNull(index)?.snapshot()?.links?.firstOrNull()?.takeIf { it.finite() }
                val center: Vector3f
                val bodyRotation: Quaternion
                if (frame != null) {
                    center = Vector3f(
                        frame.position.x.toFloat(),
                        frame.position.y.toFloat(),
                        frame.position.z.toFloat(),
                    )
                    bodyRotation = Quaternion(frame.rotationX, frame.rotationY, frame.rotationZ, frame.rotationW)
                } else {
                    val local = Vector3f(run.center.x.toFloat(), run.center.y.toFloat(), run.center.z.toFloat())
                    center = matrix.mult(local, Vector3f()).addLocal(origin)
                    bodyRotation = Quaternion(rotation).mult(
                        Quaternion(run.rotation.x, run.rotation.y, run.rotation.z, run.rotation.w),
                    )
                }
                TrackedCollisionDebug.appendBox(
                    segments,
                    center,
                    bodyRotation,
                    Vector3f(run.halfExtents.x.toFloat(), run.halfExtents.y.toFloat(), run.halfExtents.z.toFloat()),
                    TrackedCollisionDebug.KIND_RUN,
                )
            }
        }
        val truncated = segments.size / TrackedCollisionDebug.FLOATS_PER_SEGMENT >= TrackedCollisionDebug.MAX_SEGMENTS
        collisionDebug = TrackedCollisionDebug.Snapshot(
            collisionDebugEpoch.incrementAndGet(),
            FloatArray(segments.size) { segments[it] },
            truncated,
        )
    }

    /** True when this runtime belongs to [world]; the sampler must not cross levels. */
    fun belongsTo(world: Level): Boolean = level.mcLevel === world

    /** Game-thread entry used by the auto sampler and by tests. */
    fun samplePublishedTerrain(world: Level, force: Boolean = false) {
        val pose = publishedTerrainPose ?: return
        sampleAndApplyTerrain(world, pose.center, force)
    }

    /**
     * S4: resolve drive demand, then hand it to the belt contact layer.
     *
     * The previous implementation applied a central force to the fixed wheel-end
     * bodies. A drive sprocket is a fixed mount whose wheel-end body cannot spin
     * or translate freely, so a force there is an external push on the whole
     * mechanism: it changes the vehicle's momentum even with no ground contact,
     * and it produces no load feedback or measurable slip. That is exactly the
     * "directly push the wheel/chassis" fallback the plan forbids.
     *
     * The contact path writes a per-side surface-speed target in
     * [onContactProcessed]. [contactDriveEnabled] turns that write off for tests
     * of the control law. This method itself applies no net force.
     */    fun applyDrive() {
        if (state.get() != State.ACTIVE) return
        level.assertPhysicsThread()
        val command = driveCommand
        val drivetrain = spec.drivetrain
        // Surface speed of the belt, measured on the run bodies. This is the
        // quantity a slip/surface-motion controller must compare against
        // [TrackedDriveController.Demand.targetSpeedMps]; the wheel centres are
        // fixed to the hull and would report the hull speed instead.
        val sideSpeeds = beltSurfaceSpeeds()

        val demand = if (drivetrain.hasResponse) {
            driveController.resolve(
                throttle = command.throttle,
                steer = command.steer,
                brake = command.brake,
                powered = command.powered,
                drivetrain = drivetrain,
                dtSeconds = 1.0 / PhysicsLevel.TPS,
                actualLeftMps = sideSpeeds.first,
                actualRightMps = sideSpeeds.second,
            )
        } else {
            TrackedDriveController.resolve(
                throttle = command.throttle,
                steer = command.steer,
                brake = command.brake,
                powered = command.powered,
                differential = drivetrain.differential,
                maxSpeedMps = drivetrain.maxSpeedMps,
            )
        }
        lastDemand = demand
        lastLeftDemand = demand.left
        lastRightDemand = demand.right
        appliedDriveN = 0f
        // A slept assembly must wake when the driver asks it to move, but an
        // already active body must not have its deactivation timer reset every
        // substep. That reset is what kept a parked hull twitching.
        if (demand.state != TrackedDriveController.DriveState.COAST) {
            if (!chassis.isActive) chassis.activate(true)
            for (bodies in wheelBodies.values) {
                for (body in bodies) if (!body.isActive) body.activate(true)
            }
        }

        // No central force here. Traction, if enabled, is a pre-solver contact
        // motion on belt-terrain points. A force on the run would still be an
        // external push and would move the vehicle with no ground contact.
        lastSpeedMps = (sideSpeeds.first + sideSpeeds.second) / 2.0
    }

    /** Clear the support bit at the start of a substep so a lost contact cannot stick. */
    fun beginSupportWindow() {
        publishContactWindow()
        beltSupported = false
    }

    /** True when a belt run touched terrain during the current substep. */
    fun beltSupported(): Boolean = beltSupported

    /** Terrain normal impulse from the last completed substep, N·s. Not a joint force. */
    fun sideTerrainImpulseNs(): Pair<Double, Double> {
        val left = sideWindows[TrackedSide.LEFT]?.normalImpulseNs ?: 0.0
        val right = sideWindows[TrackedSide.RIGHT]?.normalImpulseNs ?: 0.0
        return left to right
    }

    /** Last completed per-side contact window. Empty until the first substep finishes. */
    fun lastSideContact(side: TrackedSide): SideContactWindow =
        sideWindows[side] ?: SideContactWindow.EMPTY

    /**
     * Wheel centers versus their bind pose. Physics thread only.
     *
     * This is a measurement for the static-mechanism check, not a force and not
     * a placement correction.
     */
    fun sampleStaticGear(): StaticGearSample {
        level.assertPhysicsThread()
        val (origin, rotation) = chassisPose()
        val matrix = rotation.toRotationMatrix()
        val samples = ArrayList<WheelSample>()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side).wheels
            val bodies = wheelBodies[side] ?: continue
            compiled.forEachIndexed { index, wheel ->
                val body = bodies.getOrNull(index) ?: return@forEachIndexed
                val expected = matrix.mult(
                    Vector3f(
                        wheel.bindCenter.x.toFloat(),
                        wheel.bindCenter.y.toFloat(),
                        wheel.bindCenter.z.toFloat(),
                    ),
                    Vector3f(),
                ).addLocal(origin)
                val actual = body.getPhysicsLocation(Vector3f())
                val delta = actual.subtract(expected)
                val axis = wheel.tensioner?.axis
                val axisWorld = if (axis == null) {
                    Vector3f()
                } else {
                    matrix.mult(
                        Vector3f(axis.x.toFloat(), axis.y.toFloat(), axis.z.toFloat()),
                        Vector3f(),
                    )
                }
                val axisLength = axisWorld.length().toDouble()
                val projection = if (axisLength > 1.0e-6) delta.dot(axisWorld) / axisLength else 0.0
                val along = if (axisLength > 1.0e-6) axisWorld.mult(projection.toFloat() / axisWorld.length()) else Vector3f()
                val orthogonal = delta.subtract(along).length().toDouble()
                samples += WheelSample(
                    side = side,
                    index = index,
                    role = wheel.role,
                    bindErrorM = actual.distance(expected).toDouble(),
                    actualY = actual.y.toDouble(),
                    tensionTravelM = wheel.tensioner?.travel,
                    tensionAxisLength = axis?.length() ?: 0.0,
                    axisProjectionM = projection,
                    orthogonalErrorM = orthogonal,
                )
            }
        }
        return StaticGearSample(samples, runtimes.values.sumOf { it.size })
    }

    /**
     * Belt-terrain contact, written during collision dispatch.
     *
     * Timing, verified against Bullet's solver and the shipped Libbulletjme
     * (Core 1.3.3 pins 23.2.0):
     *
     * - `onContactProcessed` is invoked from `btPersistentManifold::refreshContactPoints`.
     *   The algorithm calls that from `processCollision`, i.e. inside
     *   `performDiscreteCollisionDetection`, before `solveConstraints`.
     * - `onContactConceived` is invoked from `btManifoldResult::addContactPoint`
     *   **before** the point enters the manifold, so it also covers a brand-new
     *   point's first substep. Both callbacks share [applyBeltContact].
     * - `m_contactMotion1` is only read later by
     *   `btSequentialImpulseConstraintSolver::convertContact` when it builds the
     *   friction row, then used as the row's target relative velocity.
     *
     * The normal is never rewritten; only the tangential target, friction
     * coefficient and lateral friction directions are.
     */
    fun onContactProcessed(event: PhysicsContactEvent) {
        applyBeltContact(event.bodyA, event.bodyB, event.pointA.nativePointId(), "processed")
    }

    /**
     * First-chance variant for a contact point that is about to be created.
     *
     * Bullet has already computed the normal and local points but has not yet
     * stored the point, so a target written here applies to the same substep's
     * solver. This is what covers a brand-new belt-terrain contact;
     * [onContactProcessed] only covers points that survived the previous
     * refresh.
     */
    fun onContactConceived(event: PhysicsContactConceivedEvent) {
        applyBeltContact(event.bodyA, event.bodyB, event.pointA.nativePointId(), "conceived")
    }

    /**
     * Shared belt-terrain contact configuration.
     *
     * Both callbacks must behave identically, or a fresh contact would get a
     * different target than a persistent one. The only difference is timing.
     */
    private fun applyBeltContact(
        bodyA: PhysicsCollisionObject,
        bodyB: PhysicsCollisionObject,
        pointId: Long,
        phase: String,
    ) {
        if (state.get() != State.ACTIVE) return
        val runIsA = runSides.containsKey(bodyA)
        val runBody = when {
            runIsA -> bodyA
            runSides.containsKey(bodyB) -> bodyB
            else -> return
        }
        val other = if (runBody === bodyA) bodyB else bodyA
        if (!isTerrainMesh(other)) return
        beltSupported = true
        if (!contactDriveEnabled) return
        val side = runSides[runBody] ?: return
        val demand = currentDemand()
        val normal = Vector3f()
        ManifoldPoints.getNormalWorldOnB(pointId, normal)
        if (normal.lengthSquared() < 1.0e-8f) return
        normal.normalize()
        val chassisRotation = chassis.getPhysicsRotation(null).toRotationMatrix()
        val forward = TrackedVehicleFrame.worldForward(chassisRotation)
        if (forward.lengthSquared() < 1.0e-8f) return
        forward.normalize()
        val chassisForward = Vec3(forward.x.toDouble(), forward.y.toDouble(), forward.z.toDouble())
        val normalVec = Vec3(normal.x.toDouble(), normal.y.toDouble(), normal.z.toDouble())
        // Motion is the desired velocity of A relative to B along dir1.
        // Flip when the belt is body B so a positive target is still "forward".
        val sign = if (runBody === bodyA) 1f else -1f
        val point = Vector3f()
        if (runBody === bodyA) ManifoldPoints.getPositionWorldOnA(pointId, point)
        else ManifoldPoints.getPositionWorldOnB(pointId, point)
        // The chassis projection is the normal drive direction. A head-on riser
        // has no projection at all; only the bounded assist may then choose the
        // uphill tangent. No direction is fabricated here, because a vector
        // parallel to the normal would build invalid friction rows.
        val projected = TrackedContactDrive.projectOntoPlane(normalVec, chassisForward)
        val measuring = projected ?: chassisForward
        val tangentSpeed = tangentialSpeed(
            runBody,
            Vector3f(measuring.x.toFloat(), measuring.y.toFloat(), measuring.z.toFloat()),
            point,
            runBody === bodyA,
        )
        if (demand.state == TrackedDriveController.DriveState.COAST) {
            // Free roll: no conveyor target and no fabricated friction change.
            //
            // Two velocity-target implementations were tried here and both were
            // wrong in this mechanism, measurably:
            //  - target = the run's own surface speed decayed by
            //    exp(-loss/TPS). The run is constrained and reports far less
            //    than the hull's travel, so holding the belt at that value
            //    braked the vehicle (0.98 -> 0.15 m/s in 20 ticks).
            //  - target = the hull's speed at the contact. That drives the run
            //    toward the hull speed, i.e. it is a throttle, and the vehicle
            //    accelerated by 18% over 120 ticks after the driver released.
            //
            // Rolling resistance is therefore NOT implemented. Clearing the
            // conveyor target leaves Bullet's own contact friction to act and
            // adds no energy; the missing free-roll loss is recorded as an open
            // S4 item instead of being faked with a target that pushes.
            clearContactMotion(pointId)
            lastCoastTarget = 0f
            lastContactTarget = 0f
            recordContact(side, 0.0, tangentSpeed, 0.0)
            return
        }
        val commanded = if (demand.state == TrackedDriveController.DriveState.BRAKE) {
            0.0
        } else if (side == TrackedSide.LEFT) {
            demand.leftTargetSpeedMps
        } else {
            demand.rightTargetSpeedMps
        }
        if (!commanded.isFinite()) return
        val braking = demand.state == TrackedDriveController.DriveState.BRAKE
        // MCSP bound: only a side already slipping is limited by its measured
        // traction. A side that is merely accelerating normally has less
        // traction than the friction cap by construction; scaling it there
        // would create a false feedback loop instead of reproducing MCSP.
        val target = if (spec.drivetrain.tractionLimit &&
            demand.state == TrackedDriveController.DriveState.DRIVE
        ) {
            // MCSP reads the previous step's traction before it writes the next
            // surface speed. Use the same one-substep-delayed observation: the
            // force comes from resolved-contact impulses, never from a guessed
            // mass-times-slip product.
            val window = lastSideContact(side)
            val craftSpeed = hullPointSpeed(point, Vector3f(measuring.x.toFloat(), measuring.y.toFloat(), measuring.z.toFloat()), runBody === bodyA)
            if (window.contactCount > 0 && TrackedDriveController.slipping(commanded, tangentSpeed, craftSpeed)) {
                val loadN = window.normalImpulseNs * PhysicsLevel.TPS
                val tractionN = window.tractionImpulseNs * PhysicsLevel.TPS
                val grip = TrackedDriveController.tractionGripFraction(
                    tractionN = tractionN,
                    supportedN = loadN,
                    friction = contactFriction(side, braking),
                )
                commanded * grip
            } else {
                commanded
            }
        } else {
            commanded
        }
        // The chassis projection is the only drive direction. There is no
        // uphill tangent and no contact-geometry rewrite; climbing is produced
        // by the terrain mesh itself.
        val preferred = projected
        if (preferred == null) {
            // Pressed head-on into a face with no assist: leave the contact's
            // own friction alone instead of writing a fabricated direction, but
            // clear any target a previous substep left here. Keeping it would
            // keep propelling the belt with a command the driver no longer asks
            // for.
            clearContactMotion(pointId)
            lastContactTarget = 0f
            recordContact(side, 0.0, tangentSpeed, 0.0)
            return
        }
        val frame = TrackedContactDrive.contactFrame(normalVec, preferred)
        if (frame == null) {
            clearContactMotion(pointId)
            lastContactTarget = 0f
            recordContact(side, 0.0, tangentSpeed, 0.0)
            return
        }
        val finalTarget = target
        val friction = (contactFriction(side, braking) / sqrt(2.0)).toFloat()
        ManifoldPoints.setCombinedFriction(pointId, friction)
        ManifoldPoints.setLateralFrictionDir1(pointId, frame.dir1.toBulletVector())
        ManifoldPoints.setLateralFrictionDir2(pointId, frame.dir2.toBulletVector())
        lastContactTarget = finalTarget.toFloat()
        ManifoldPoints.setContactMotion1(pointId, finalTarget.toFloat() * sign)
        ManifoldPoints.setContactMotion2(pointId, 0f)
        ManifoldPoints.setFlags(pointId, ManifoldPoints.getFlags(pointId) or ContactPointFlag.LATERAL_FRICTION)
        contactMotions += 1
        if (phase == "conceived") conceivedMotions += 1
        // Sample only a driven contact: coast and brake write a zero target and
        // would otherwise be counted as enormous slip.
        if (finalTarget > 0.0) {
            slipAccum.computeIfAbsent(side) { SlipAccum() }.add(tangentSpeed - finalTarget, finalTarget)
            lastContactDir1 = Vec3(frame.dir1.x, frame.dir1.y, frame.dir1.z)
        }
        recordContact(side, 0.0, tangentSpeed, finalTarget)
    }

    /**
     * Velocity of [body] at a world [point], projected on [direction] in the
     * relative sense Bullet uses for `contactMotion1`.
     *
     * Kept for diagnostics and the traction comparison. It is deliberately not
     * used as the coast target: the point term of a constrained run/hull pair
     * does not describe the vehicle's travel and using it injected energy.
     */
    private fun hullPointSpeed(
        point: Vector3f,
        direction: Vector3f,
        runIsA: Boolean,
    ): Double {
        val angular = chassis.getAngularVelocity(null)
        val lever = point.subtract(chassis.getPhysicsLocation(null), Vector3f())
        val velocity = chassis.getLinearVelocity(null).add(angular.cross(lever, Vector3f()))
        val along = velocity.dot(direction).toDouble()
        // For a belt-terrain contact the other body is static terrain, so the
        // relative surface speed the row targets is the belt's own ground speed.
        return if (runIsA) along else -along
    }

    /**
     * Demand the contact path should write.
     *
     * With the MCSP response layer enabled, [lastDemand] is the exact value the
     * stateful controller produced for this substep; recomputing would advance
     * the steer lever and slew twice. Without it the historical stateless
     * resolution is kept, because a bare runtime fixture may drive contacts
     * without ever calling [applyDrive].
     */
    private fun currentDemand(): TrackedDriveController.Demand {
        if (spec.drivetrain.hasResponse) return lastDemand
        val command = driveCommand
        return TrackedDriveController.resolve(
            command.throttle,
            command.steer,
            command.brake,
            command.powered,
            spec.drivetrain.differential,
            spec.drivetrain.maxSpeedMps,
        )
    }

    private fun Vec3.toBulletVector(): Vector3f =
        Vector3f(x.toFloat(), y.toFloat(), z.toFloat())

    private fun clearContactMotion(pointId: Long) {
        ManifoldPoints.setContactMotion1(pointId, 0f)
        ManifoldPoints.setContactMotion2(pointId, 0f)
        ManifoldPoints.setFlags(pointId, ManifoldPoints.getFlags(pointId) and ContactPointFlag.LATERAL_FRICTION.inv())
    }

    private fun contactFriction(side: TrackedSide, braking: Boolean): Double {
        val torque = if (braking) spec.drivetrain.maxBrakeTorqueNm else spec.drivetrain.maxDriveTorqueNm
        val radius = spec.side(side).wheels.firstOrNull { it.receivesDrive }?.radius ?: 0.3
        return TrackedContactDrive.frictionCoefficient(
            authoredFriction = spec.drivetrain.friction,
            torqueNm = torque,
            radiusM = radius,
            supportedMassKg = spec.massKg / 2.0,
        )
    }

    private fun tangentialSpeed(
        runBody: PhysicsCollisionObject,
        forward: Vector3f,
        point: Vector3f,
        runIsA: Boolean,
    ): Double {
        val rigid = runBody as? PhysicsRigidBody ?: return 0.0
        if (!rigid.isDynamic) return 0.0
        val velocity = rigid.getLinearVelocity(null).add(
            rigid.getAngularVelocity(null).cross(point.subtract(rigid.getPhysicsLocation(null)), Vector3f()),
        )
        val along = velocity.dot(forward).toDouble()
        return if (runIsA) along else -along
    }

    private fun recordContact(side: TrackedSide, normalImpulse: Double, tangentSpeed: Double, target: Double) {
        val accum = sideAccum.computeIfAbsent(side) { SideAccum() }
        accum.count += 1
        accum.tangent += tangentSpeed
        accum.target += target
        accum.normal += normalImpulse
    }

    private fun publishContactWindow() {
        val published = TrackedSide.entries.associateWith { side ->
            val accum = sideAccum[side] ?: return@associateWith SideContactWindow.EMPTY
            val tangent = if (accum.count == 0) 0.0 else accum.tangent / accum.count
            val target = if (accum.count == 0) 0.0 else accum.target / accum.count
            SideContactWindow(
                normalImpulseNs = accum.normal,
                tangentialSpeedMps = tangent,
                targetSpeedMps = target,
                slipMps = tangent - target,
                contactCount = accum.count,
                tractionImpulseNs = accum.traction,
            )
        }
        sideWindows = published
        sideAccum.values.forEach { it.reset() }
        publishSupportWindow()
    }

    /** Last completed terrain-support split. Empty until a substep finishes. */
    fun lastSupportContacts(): SupportContactWindow = supportContacts

    private fun noteTerrainSupport(
        source: SupportSource,
        side: TrackedSide?,
        event: cn.solarmoon.spark_core.physics.body.PhysicsBodyEvent.ResolvedContact,
    ) {
        if (!isTerrainMesh(event.other)) return
        val impulse = event.snapshot.normalImpulse
        if (impulse <= 0f || !impulse.isFinite() || event.snapshot.normal.y <= 0.4f) return
        noteSupport(
            source,
            side,
            impulse.toDouble(),
            tangentialForwardNs(event.snapshot.impulse, event.snapshot.normal),
        )
    }

    private fun noteRunSupport(side: TrackedSide, impulseNs: Double, tangentNs: Double) {
        noteSupport(SupportSource.RUN, side, impulseNs, tangentNs)
    }

    private fun noteSupport(source: SupportSource, side: TrackedSide?, impulseNs: Double, tangentNs: Double) {
        if (impulseNs <= 0.0 || !impulseNs.isFinite()) return
        val tangent = if (tangentNs.isFinite()) tangentNs else 0.0
        when (source) {
            SupportSource.CHASSIS -> {
                chassisSupportCount += 1
                chassisSupportImpulse += impulseNs
            }
            SupportSource.RUN, SupportSource.WHEEL -> {
                val accum = side?.let { sideSupport[it] } ?: return
                if (source == SupportSource.RUN) {
                    accum.runCount += 1
                    accum.runImpulse += impulseNs
                    accum.runTangent += tangent
                } else {
                    accum.wheelCount += 1
                    accum.wheelImpulse += impulseNs
                    accum.wheelTangent += tangent
                }
            }
        }
    }

    /**
     * Friction impulse on this body, projected on the chassis nose.
     *
     * Positive means the ground pushed the body forward. A locked road wheel
     * sliding on the mesh shows up as a tangent opposing the run's tangent.
     * This is an observation, not a torque and not a pass threshold.
     */
    private fun tangentialForwardNs(impulse: Vec3, normal: Vec3): Double {
        val alongNormal = impulse.x * normal.x + impulse.y * normal.y + impulse.z * normal.z
        val tx = impulse.x - normal.x * alongNormal
        val ty = impulse.y - normal.y * alongNormal
        val tz = impulse.z - normal.z * alongNormal
        val forward = TrackedVehicleFrame.worldForward(chassis.getPhysicsRotation(null).toRotationMatrix())
        return tx * forward.x + ty * forward.y + tz * forward.z
    }

    private fun publishSupportWindow() {
        val left = sideSupport.getValue(TrackedSide.LEFT).snapshot()
        val right = sideSupport.getValue(TrackedSide.RIGHT).snapshot()
        val heights = sampleSupportHeights()
        val motion = sampleChassisMotion()
        val window = SupportContactWindow(
            left = left,
            right = right,
            chassisCount = chassisSupportCount,
            chassisImpulseNs = chassisSupportImpulse,
            lowestRunY = heights.lowestRunY,
            lowestWheelBottomY = heights.lowestWheelBottomY,
            chassisBottomY = heights.chassisBottomY,
            terrainFloorMinY = terrainProxy.installedFloorMinY,
            terrainFloorMaxY = terrainProxy.installedFloorMaxY,
            forwardSpeedMps = motion.first,
            yawRateRadPerSec = motion.second,
        )
        supportContacts = window
        sideSupport.values.forEach { it.reset() }
        chassisSupportCount = 0
        chassisSupportImpulse = 0.0
        val wheelsThroughFloor = window.terrainFloorMinY.isFinite() &&
            window.lowestWheelBottomY.isFinite() &&
            window.lowestWheelBottomY < window.terrainFloorMinY - 0.15
        val hullOnly = window.chassisImpulseNs > 1.0 && window.runImpulseNs <= 0.0 && window.wheelImpulseNs <= 0.0
        val wheelDrag = lastDemand.state == TrackedDriveController.DriveState.DRIVE &&
            window.wheelImpulseNs > 1.0 &&
            abs(window.wheelTangentNs) > abs(window.runTangentNs) + 1.0
        if (hullOnly || wheelsThroughFloor || wheelDrag) {
            val now = level.tickCount
            if (now - lastSupportWarnTick >= PhysicsLevel.TPS) {
                lastSupportWarnTick = now
                com.atsuishio.superbwarfare.Mod.LOGGER.warn(
                    "[tracked-support] {} support split: {}",
                    spec.vehicleId,
                    window,
                )
            }
        }
    }

    private fun sampleSupportHeights(): SupportHeights {
        var lowestRun = Double.POSITIVE_INFINITY
        var lowestWheel = Double.POSITIVE_INFINITY
        for (body in runSides.keys) {
            val y = lowestPointY(body) ?: continue
            if (y < lowestRun) lowestRun = y
        }
        for (bodies in wheelBodies.values) {
            for (body in bodies) {
                val y = lowestPointY(body) ?: continue
                if (y < lowestWheel) lowestWheel = y
            }
        }
        return SupportHeights(
            lowestRunY = lowestRun.takeIf { it.isFinite() } ?: Double.NaN,
            lowestWheelBottomY = lowestWheel.takeIf { it.isFinite() } ?: Double.NaN,
            chassisBottomY = lowestPointY(chassis) ?: Double.NaN,
        )
    }

    private fun sampleChassisMotion(): Pair<Double, Double> {
        if (!chassis.isInWorld) return Double.NaN to Double.NaN
        val rotation = chassis.getPhysicsRotation(null).toRotationMatrix()
        val forward = TrackedVehicleFrame.worldForward(rotation)
        val linear = chassis.getLinearVelocity(null)
        val forwardSpeed = linear.x * forward.x + linear.y * forward.y + linear.z * forward.z
        val up = rotation.mult(Vector3f(0f, 1f, 0f), Vector3f())
        val angular = chassis.getAngularVelocity(null)
        val yawRate = angular.x * up.x + angular.y * up.y + angular.z * up.z
        return forwardSpeed.toDouble() to yawRate.toDouble()
    }

    private fun lowestPointY(body: PhysicsRigidBody): Double? {
        if (!body.isInWorld) return null
        val box = body.boundingBox(null) ?: return null
        return box.getMin(null).y.toDouble()
    }

    /** How many belt-terrain points received a contact-motion target this session. */
    fun contactMotionCount(): Int = contactMotions

    /**
     * How many of those targets were written before the point entered the
     * manifold.
     *
     * A non-zero value is the observable difference between the first-chance
     * hook and the refresh-only path: only this counter can cover a brand-new
     * contact on its first substep.
     */
    fun conceivedContactMotionCount(): Int = conceivedMotions

    /** Last belt-terrain contact-motion target, m/s. Positive is forward. */
    fun lastContactTarget(): Float = lastContactTarget

    /** `lateralFrictionDir1` written on the last driven contact. */
    fun lastContactDir1(): Vec3 = lastContactDir1

    /** Last free-roll target written while coasting, m/s. Zero means no loss. */
    fun lastCoastTarget(): Float = lastCoastTarget

    /** Number of real coasting contacts that received a free-roll target. */
    fun coastContactCount(): Int = coastContacts

    /**
     * A contact belongs to the vehicle's terrain authority when it is the
     * private bullet mesh. It is intentionally owned by the same host, so an
     * owner check would hide exactly the terrain this runtime must use.
     */
    private fun isTerrainMesh(body: PhysicsCollisionObject): Boolean =
        body.collisionGroup and TrackedTerrainCollision.TERRAIN_MESH != 0

    private val contactListener = object {
        @SubscribeEvent
        fun onContact(event: PhysicsContactEvent) {
            onContactProcessed(event)
        }

        @SubscribeEvent
        fun onConceived(event: PhysicsContactConceivedEvent) {
            onContactConceived(event)
        }
    }

    private var supportWindow: Subscription? = null

    private fun registerSupportWindow() {
        if (supportWindow != null) return
        supportWindow = chassis.onPrePhysicsTick {
            if (state.get() == State.ACTIVE) {
                // This hook is the single publisher of the plain wheel pose. A
                // bare runtime (a GameTest or a future non-entity owner) has no
                // other caller, and the game-thread terrain sampler would
                // otherwise see an empty wheel list forever. Publishing from
                // here only, not also from VehicleBody.preStep, keeps it to one
                // native read per substep.
                publishStepPose()
                beginSupportWindow()
            }
        }
    }

    private fun registerContactListener() {
        if (contactRegistered) return
        MinecraftForge.EVENT_BUS.register(contactListener)
        contactRegistered = true
    }

    private fun unregisterContactListener() {
        if (!contactRegistered) return
        MinecraftForge.EVENT_BUS.unregister(contactListener)
        contactRegistered = false
    }

    /**
     * Mean forward speed of each side's belt, from the run bodies.
     *
     * Read on the physics thread. When a run is unavailable the side reads zero,
     * which is an honest "unknown/stopped" value rather than a stale speed.
     */
    private fun beltSurfaceSpeeds(): Pair<Double, Double> {
        val forward = TrackedVehicleFrame.worldForward(chassis.getPhysicsRotation(null).toRotationMatrix())
        fun sideSpeed(side: TrackedSide): Double {
            val samples = runtimes[side].orEmpty().mapNotNull { runtime ->
                runtime.linkVelocity(0)?.let { velocity ->
                    velocity.x * forward.x + velocity.y * forward.y + velocity.z * forward.z
                }
            }
            return if (samples.isEmpty()) 0.0 else samples.average()
        }
        return sideSpeed(TrackedSide.LEFT) to sideSpeed(TrackedSide.RIGHT)
    }

    /**
     * Pre-solver belt contact motion. A straight-ahead GameTest showed the hull
     * moves from this path alone. Brake and steering use the same write; they
     * are not separately proven. Tests of the control law turn this off.
     */
    @Volatile var contactDriveEnabled = true

    /**
     * Test hook. When set, [create] fails after the wheel bodies exist so the
     * rollback ledger can be observed. Not a production control.
     */
    @Volatile var failCreateAfterBodies = false

    /** Test hook. Fails after run runtimes are recorded, before end joints. */
    @Volatile var failCreateAfterRuns = false

    /** Test hook. Fails after runs exist and before external end joints are created. */
    @Volatile var failCreateAfterEnds = false

    data class WheelSample(
        val side: TrackedSide,
        val index: Int,
        val role: TrackWheelRole,
        val bindErrorM: Double,
        val actualY: Double,
        val tensionTravelM: Double?,
        val tensionAxisLength: Double,
        val axisProjectionM: Double = 0.0,
        val orthogonalErrorM: Double = 0.0,
    )

    data class StaticGearSample(
        val wheels: List<WheelSample>,
        val runCount: Int,
    )

    /**
     * Terrain support split for one completed substep.
     *
     * A sunk hull shows up here as chassis impulse with no run or wheel impulse.
     * These are contact observations, not joint forces and not a pass threshold.
     */
    data class SideSupportWindow(
        val runCount: Int = 0,
        val runImpulseNs: Double = 0.0,
        val wheelCount: Int = 0,
        val wheelImpulseNs: Double = 0.0,
        /** Friction impulse along the chassis nose, N·s. Positive pushes forward. */
        val runTangentNs: Double = 0.0,
        /** Same projection for wheel-terrain friction. A locked wheel drags here. */
        val wheelTangentNs: Double = 0.0,
    )

    data class SupportContactWindow(
        val left: SideSupportWindow = SideSupportWindow(),
        val right: SideSupportWindow = SideSupportWindow(),
        val chassisCount: Int = 0,
        val chassisImpulseNs: Double = 0.0,
        /** World Y of the lowest run-box point. NaN before the first sample. */
        val lowestRunY: Double = Double.NaN,
        /** World Y of the lowest wheel-collider point. */
        val lowestWheelBottomY: Double = Double.NaN,
        /** World Y of the lowest chassis-collider point. */
        val chassisBottomY: Double = Double.NaN,
        /** Lowest upward face of the installed terrain mesh, world Y. */
        val terrainFloorMinY: Double = Double.NaN,
        /** Highest upward face of the installed terrain mesh, world Y. */
        val terrainFloorMaxY: Double = Double.NaN,
        /** Chassis velocity along its nose, m/s. NaN before the body is in the world. */
        val forwardSpeedMps: Double = Double.NaN,
        /** Chassis angular velocity about its up axis, rad/s. */
        val yawRateRadPerSec: Double = Double.NaN,
    ) {
        val runCount: Int get() = left.runCount + right.runCount
        val runImpulseNs: Double get() = left.runImpulseNs + right.runImpulseNs
        val runTangentNs: Double get() = left.runTangentNs + right.runTangentNs
        val wheelCount: Int get() = left.wheelCount + right.wheelCount
        val wheelImpulseNs: Double get() = left.wheelImpulseNs + right.wheelImpulseNs
        val wheelTangentNs: Double get() = left.wheelTangentNs + right.wheelTangentNs

        companion object {
            val EMPTY = SupportContactWindow()
        }
    }

    private data class SupportHeights(
        val lowestRunY: Double,
        val lowestWheelBottomY: Double,
        val chassisBottomY: Double,
    )

    private enum class SupportSource { RUN, WHEEL, CHASSIS }

    private class SideSupportAccum {
        var runCount = 0
        var runImpulse = 0.0
        var runTangent = 0.0
        var wheelCount = 0
        var wheelImpulse = 0.0
        var wheelTangent = 0.0

        fun snapshot() = SideSupportWindow(
            runCount, runImpulse, wheelCount, wheelImpulse, runTangent, wheelTangent,
        )

        fun reset() {
            runCount = 0
            runImpulse = 0.0
            runTangent = 0.0
            wheelCount = 0
            wheelImpulse = 0.0
            wheelTangent = 0.0
        }
    }

    /** One completed substep of belt-terrain contact on one side. */
    data class SideContactWindow(
        val normalImpulseNs: Double,
        val tangentialSpeedMps: Double,
        val targetSpeedMps: Double,
        val slipMps: Double,
        val contactCount: Int,
        /**
         * Sum of resolved contact impulses projected on the chassis forward
         * axis, N·s. This is the measured longitudinal traction for the
         * completed substep, not a commanded force. Positive means the contact
         * pushed the vehicle forward.
         */
        val tractionImpulseNs: Double = 0.0,
    ) {
        companion object {
            val EMPTY = SideContactWindow(0.0, 0.0, 0.0, 0.0, 0)
        }
    }

    private class SideAccum {
        var normal = 0.0
        var tangent = 0.0
        var target = 0.0
        var count = 0
        var traction = 0.0
        fun reset() {
            normal = 0.0
            tangent = 0.0
            target = 0.0
            count = 0
            traction = 0.0
        }
    }

    private val runSides = java.util.concurrent.ConcurrentHashMap<PhysicsRigidBody, TrackedSide>()
    private val sideAccum = java.util.concurrent.ConcurrentHashMap<TrackedSide, SideAccum>()
    @Volatile private var sideWindows: Map<TrackedSide, SideContactWindow> = emptyMap()
    private val sideSupport = TrackedSide.entries.associateWith { SideSupportAccum() }
    private var chassisSupportCount = 0
    private var chassisSupportImpulse = 0.0
    @Volatile private var supportContacts: SupportContactWindow = SupportContactWindow.EMPTY
    private var lastSupportWarnTick = Long.MIN_VALUE
    @Volatile private var beltSupported = false
    @Volatile private var contactMotions = 0
    @Volatile private var conceivedMotions = 0
    @Volatile private var lastContactTarget = 0f
    /** `lateralFrictionDir1` actually written on the last driven contact. */
    @Volatile private var lastContactDir1: Vec3 = Vec3.ZERO
    /** Last free-roll target written on a coasting belt contact, m/s. */
    @Volatile private var lastCoastTarget = 0f
    @Volatile private var coastContacts = 0
    @Volatile private var contactRegistered = false

    /**
     * Bounded slip observation for one side.
     *
     * `slipMps` is `tangentialSpeed - targetSpeed`, so it is a *measurement* of
     * how far the belt is from its commanded surface speed, not an error to
     * drive to zero. A tracked vehicle under load always slips; the question a
     * gate can answer is whether that slip stays inside a declared band.
     *
     * Sampled only while the side has a nonzero contact target, so coast and
     * braking do not pollute the band.
     */
    data class SlipBand(
        val samples: Int,
        val minMps: Double,
        val maxMps: Double,
        val meanMps: Double,
        /** Largest target observed while sampling; 0 when nothing was driven. */
        val maxTargetMps: Double = 0.0,
    ) {
        companion object {
            val EMPTY = SlipBand(0, 0.0, 0.0, 0.0, 0.0)
        }
    }

    private class SlipAccum {
        var samples = 0
        var sum = 0.0
        var min = Double.POSITIVE_INFINITY
        var max = Double.NEGATIVE_INFINITY
        var maxTarget = 0.0
        fun add(value: Double, target: Double) {
            if (!value.isFinite()) return
            samples += 1
            sum += value
            if (value < min) min = value
            if (value > max) max = value
            if (target > maxTarget) maxTarget = target
        }
        fun snapshot(): SlipBand =
            if (samples == 0) SlipBand.EMPTY
            else SlipBand(samples, min, max, sum / samples, maxTarget)
    }

    private val slipAccum = java.util.concurrent.ConcurrentHashMap<TrackedSide, SlipAccum>()

    /** Reset the slip band so a test can measure one steady phase only. */
    fun resetSlipBand() {
        slipAccum.clear()
    }

    /** Slip band observed since the last [resetSlipBand], per side. */
    fun slipBand(side: TrackedSide): SlipBand =
        slipAccum[side]?.snapshot() ?: SlipBand.EMPTY

    companion object {
        /** Ticks before a failed terrain region request may be retried. */
        private const val RETRY_BACKOFF_TICKS = 10L
        private val NEXT_TERRAIN_REGION_ID = java.util.concurrent.atomic.AtomicLong()

        /**
         * Active runtimes are the only game-thread sampling targets. The
         * auto-sampler never scans the Level for hidden physics objects.
         */
        internal val ACTIVE_RUNTIMES: MutableSet<TrackedVehicleRuntime> =
            java.util.concurrent.ConcurrentHashMap.newKeySet()
    }

}
